package io.quarkus.qute.runtime.devmode;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.dev.ErrorPageGenerators;
import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.HotReplacementSetup;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Escaper;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.ReflectionValueResolver;
import io.quarkus.qute.Template;
import io.quarkus.qute.ValueResolver;
import io.quarkus.runtime.TemplateHtmlBuilder;

public class QuteErrorPageSetup implements HotReplacementSetup {

    private static final Logger LOG = Logger.getLogger(QuteErrorPageSetup.class);

    private static final String TEMPLATE_EXCEPTION = "io.quarkus.qute.TemplateException";
    private static final String ORIGIN = "io.quarkus.qute.TemplateNode$Origin";

    private static final String PROBLEM_TEMPLATE = ""
            + "<h3>#{problemIndex} {title}</h3>\n"
            + "<div style=\"margin-bottom:0.5em;\">{description}</div>\n"
            + "<div style=\"font-family:monospace;font-size:1em;background-color:#2E3436;color:white;padding:1em;margin-bottom:2em;\">\n"
            + "{#if realLines.get(0) > 1}<span style=\"color:silver;\">...</span><br>{/if}\n"
            + "{#for line in sourceLines}\n"
            // highlight the error line - start
            + "{#if lineNumber is realLines.get(index)}<div style=\"background-color:#555753;\">{/if}\n"
            // line number
            + "<span style=\"color:silver;\">{realLines.get(index).pad}</span>\n"
            // line content
            + " {line}\n"
            // highlight the error line - end 
            + "{#if lineNumber is realLines.get(index)}</div>{#else}<br>{/if}\n"
            // point to error
            + "{#if lineNumber is realLines.get(index)}{space.pad}<span style=\"color:red;\">{#for i in lineCharacterStart}={/for}^</span><br>{/if}\n"
            + "{/for}\n"
            + "{#if endLinesSkipped}<span style=\"color:silver;\">...</span>{/if}\n"
            + "</div>";

    private HotReplacementContext hotReplacementContext;

    @Override
    public void setupHotDeployment(HotReplacementContext context) {
        this.hotReplacementContext = context;
        ErrorPageGenerators.register(TEMPLATE_EXCEPTION, this::generatePage);
    }

    String generatePage(Throwable exception) {
        Escaper escaper = Escaper.builder().add('"', "&quot;").add('\'', "&#39;")
                .add('&', "&amp;").add('<', "&lt;").add('>', "&gt;").build();
        Template problemTemplate = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver())
                .addValueResolver(new ValueResolver() {

                    public boolean appliesTo(EvalContext context) {
                        return context.getName().equals("pad");
                    }

                    @Override
                    public CompletionStage<Object> resolve(EvalContext context) {
                        return CompletableFuture.completedFuture(htmlPadRight(context.getBase().toString(), 5));
                    }
                })
                .build()
                .parse(PROBLEM_TEMPLATE);
        TemplateHtmlBuilder builder;
        List<Throwable> problems;
        Throwable[] suppressed = exception.getSuppressed();
        if (suppressed.length == 0) {
            problems = Collections.singletonList(exception);
        } else {
            problems = Arrays.asList(suppressed);
        }

        String problemsFound = "Found " + problems.size() + " Qute problems";
        builder = new TemplateHtmlBuilder("Error restarting Quarkus", problemsFound, problemsFound);

        // Attempt to sort problems by line
        Collections.sort(problems, new Comparator<Throwable>() {
            @Override
            public int compare(Throwable t1, Throwable t2) {
                Object o1 = getOrigin(t1);
                Object o2 = getOrigin(t2);
                if (o1 == o2) {
                    return 0;
                } else if (o1 == null && o2 != null) {
                    return -1;
                } else if (o1 != null && o2 == null) {
                    return 1;
                }
                return Integer.compare(getLine(o1), getLine(o2));
            }
        });

        for (ListIterator<Throwable> it = problems.listIterator(); it.hasNext();) {
            Throwable problem = it.next();
            builder.append(getProblemInfo(it.previousIndex() + 1, problem, problemTemplate, escaper));
        }
        return builder.toString();
    }

    String getProblemInfo(int index, Throwable problem, Template problemTemplate, Escaper escaper) {
        Object origin = getOrigin(problem);
        String[] messageLines = problem.getMessage().split("\\r?\\n");

        if (origin == null) {
            return Arrays.stream(messageLines).collect(Collectors.joining("<br>"));
        }

        String templateId = getTemplateId(origin);
        int lineNumber = getLine(origin);
        int lineCharacterStart = getLineCharacterStart(origin);

        List<String> sourceLines = new ArrayList<>();
        try (BufferedReader in = getBufferedReader(templateId)) {
            String line = null;
            while ((line = in.readLine()) != null) {
                sourceLines.add(escaper.escape(line).replace(" ", "&nbsp;"));
            }
        } catch (Exception e) {
            LOG.warn("Unable to read the template source: " + templateId, e);
        }

        List<Integer> realLines = new ArrayList<>();
        boolean endLinesSkipped = false;
        if (sourceLines.size() > 15) {
            // Line with error plus few surrounding lines
            int fromIndex = lineNumber > 7 ? (lineNumber - 8) : 0;
            int toIndex = (lineNumber + 7) > sourceLines.size() ? sourceLines.size() : lineNumber + 7;
            for (int j = fromIndex; j < toIndex; j++) {
                // e.g. [10,11,12]
                realLines.add(j + 1);
            }
            sourceLines = sourceLines.subList(fromIndex, toIndex);
            endLinesSkipped = toIndex != sourceLines.size();
        } else {
            for (int j = 0; j < sourceLines.size(); j++) {
                // [1,2,3]
                realLines.add(j);
            }
        }

        return problemTemplate
                .data("problemIndex", index)
                .data("title", messageLines[0])
                .data("description", Arrays.stream(messageLines).skip(1).collect(Collectors.joining("<br>")))
                .data("sourceLines", sourceLines)
                .data("lineNumber", lineNumber)
                .data("lineCharacterStart", lineCharacterStart)
                .data("realLines", realLines)
                .data("endLinesSkipped", endLinesSkipped)
                .data("space", " ")
                .render();
    }

    static String htmlPadRight(String s, int n) {
        return String.format("%-" + n + "s", s).replace(" ", "&nbsp;");
    }

    private BufferedReader getBufferedReader(String templateId) throws IOException {
        for (Path resource : hotReplacementContext.getResourcesDir()) {
            // src/main/resources/templates
            Path templates = resource.resolve("templates");
            if (Files.exists(templates)) {
                // src/main/resources/templates/items.html
                Path template = templates.resolve(templateId.replace("\\", "/"));
                if (Files.exists(template)) {
                    return Files.newBufferedReader(template);
                }
            }
        }
        throw new IllegalStateException("Template source not available");
    }

    private Object getOrigin(Throwable t) {
        Object origin = null;
        try {
            Method getOrigin = t.getClass().getClassLoader().loadClass(TEMPLATE_EXCEPTION).getMethod("getOrigin");
            origin = getOrigin.invoke(t);
        } catch (NoSuchMethodException | SecurityException | ClassNotFoundException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            LOG.warn("Unable to read the origin field", e);
        }
        return origin;
    }

    private String getTemplateId(Object origin) {
        String templateId = null;
        try {
            Method getTemplateId = origin.getClass().getClassLoader().loadClass(ORIGIN).getMethod("getTemplateId");
            templateId = getTemplateId.invoke(origin).toString();
        } catch (NoSuchMethodException | SecurityException | ClassNotFoundException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            LOG.warn("Unable to invoke the TemplateNode.Origin.getTemplateId() method", e);
        }
        return templateId;
    }

    private int getLine(Object origin) {
        int line = 0;
        try {
            Method getLine = origin.getClass().getClassLoader().loadClass(ORIGIN).getMethod("getLine");
            line = (int) getLine.invoke(origin);
        } catch (NoSuchMethodException | SecurityException | ClassNotFoundException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            LOG.warn("Unable to invoke the TemplateNode.Origin.getLine() method", e);
        }
        return line;
    }

    private int getLineCharacterStart(Object origin) {
        int lineCharacter = 0;
        try {
            Method getLineCharacter = origin.getClass().getClassLoader().loadClass(ORIGIN).getMethod("getLineCharacterStart");
            lineCharacter = (int) getLineCharacter.invoke(origin);
        } catch (NoSuchMethodException | SecurityException | ClassNotFoundException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException e) {
            LOG.warn("Unable to invoke the TemplateNode.Origin.getLineCharacterStart() method", e);
        }
        return lineCharacter;
    }

}
