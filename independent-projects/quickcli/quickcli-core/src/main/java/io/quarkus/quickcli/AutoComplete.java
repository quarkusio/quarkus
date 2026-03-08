package io.quarkus.quickcli;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates bash/zsh auto-completion scripts for QuickCLI-based applications.
 * <p>
 * Walks the {@link CommandSpec} hierarchy to produce a bash completion script
 * that handles subcommands, options (flags and arg-taking), and positional
 * parameters with file/hostname completion.
 * </p>
 */
public final class AutoComplete {

    private AutoComplete() {
    }

    /**
     * Generates and returns the source code for a bash/zsh autocompletion script.
     *
     * @param scriptName  the name of the command (e.g. "quarkus")
     * @param rootSpec    the root {@link CommandSpec}
     * @return bash completion script source code
     */
    public static String bash(String scriptName, CommandSpec rootSpec) {
        if (scriptName == null) {
            throw new NullPointerException("scriptName");
        }
        if (rootSpec == null) {
            throw new NullPointerException("rootSpec");
        }
        scriptName = sanitizeScriptName(scriptName);
        StringBuilder result = new StringBuilder(4096);
        result.append(format(SCRIPT_HEADER, scriptName, CommandLine.VERSION));

        List<CommandDescriptor> hierarchy = createHierarchy(scriptName, rootSpec);
        result.append(generateEntryPointFunction(scriptName, hierarchy));

        for (CommandDescriptor descriptor : hierarchy) {
            result.append(generateFunctionForCommand(descriptor.functionName,
                    descriptor.commandName, descriptor.spec));
        }
        result.append(format(SCRIPT_FOOTER, scriptName));
        return result.toString();
    }

    // --- internals ---

    private static String sanitizeScriptName(String scriptName) {
        return scriptName
                .replaceAll("\\.sh", "")
                .replaceAll("\\.bash", "")
                .replaceAll("\\.\\/", "");
    }

    private static String bashify(CharSequence value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                builder.append(c);
            } else if (Character.isSpaceChar(c)) {
                builder.append('_');
            }
        }
        if (builder.length() > 0 && Character.isDigit(builder.charAt(0))) {
            builder.insert(0, "_");
        }
        return builder.toString();
    }

    // ---- hierarchy ----

    private static final class CommandDescriptor {
        final String functionName;
        final String parentFunctionName;
        final String parentWithoutTopLevelCommand;
        final String commandName;
        final CommandSpec spec;

        CommandDescriptor(String functionName, String parentFunctionName,
                          String parentWithoutTopLevelCommand, String commandName,
                          CommandSpec spec) {
            this.functionName = functionName;
            this.parentFunctionName = parentFunctionName;
            this.parentWithoutTopLevelCommand = parentWithoutTopLevelCommand;
            this.commandName = commandName;
            this.spec = spec;
        }
    }

    private static List<CommandDescriptor> createHierarchy(String scriptName, CommandSpec rootSpec) {
        List<CommandDescriptor> result = new ArrayList<>();
        result.add(new CommandDescriptor("_picocli_" + scriptName, "", "",
                scriptName, rootSpec));
        createSubHierarchy(scriptName, "", rootSpec, result);
        return result;
    }

    private static void createSubHierarchy(String scriptName,
                                            String parentWithoutTopLevelCommand,
                                            CommandSpec parentSpec,
                                            List<CommandDescriptor> out) {
        // breadth-first: add descriptors for each subcommand
        for (Map.Entry<String, CommandSpec> entry : parentSpec.subcommands().entrySet()) {
            String commandName = entry.getKey();
            CommandSpec subSpec = entry.getValue();
            String functionNameWithoutPrefix = bashify(
                    concat("_", parentWithoutTopLevelCommand.replace(' ', '_'), commandName));
            String functionName = concat("_", "_picocli", scriptName, functionNameWithoutPrefix);
            String parentFunctionName = parentWithoutTopLevelCommand.isEmpty()
                    ? concat("_", "_picocli", scriptName)
                    : concat("_", "_picocli", scriptName,
                    bashify(parentWithoutTopLevelCommand.replace(' ', '_')));
            out.add(new CommandDescriptor(functionName, parentFunctionName,
                    parentWithoutTopLevelCommand, commandName, subSpec));
        }
        // then recurse
        for (Map.Entry<String, CommandSpec> entry : parentSpec.subcommands().entrySet()) {
            String newParent = concat(" ", parentWithoutTopLevelCommand, entry.getKey());
            createSubHierarchy(scriptName, newParent, entry.getValue(), out);
        }
    }

    // ---- entry-point function ----

    private static String generateEntryPointFunction(String scriptName,
                                                      List<CommandDescriptor> hierarchy) {
        StringBuilder buff = new StringBuilder(1024);
        buff.append(format(
                "# Bash completion entry point function.\n"
                        + "function _complete_%1$s() {\n",
                scriptName));

        // Edge case lines
        buff.append("  # Edge case: if command line has no space after subcommand, "
                + "don't assume it is selected.\n");
        for (CommandDescriptor descriptor : hierarchy.subList(1, hierarchy.size())) {
            String withoutTopLevel = concat(" ",
                    descriptor.parentWithoutTopLevelCommand, descriptor.commandName);
            buff.append(format(
                    "  if [ \"${COMP_LINE}\" = \"${COMP_WORDS[0]} %1$s\" ]; "
                            + "then %2$s; return $?; fi\n",
                    withoutTopLevel, descriptor.parentFunctionName));
        }
        buff.append("\n");

        // CompWordsContainsArray calls (longest match first)
        buff.append("  # Find the longest sequence of subcommands and call "
                + "the bash function for that subcommand.\n");
        List<String> functionCalls = new ArrayList<>();
        for (CommandDescriptor descriptor : hierarchy.subList(1, hierarchy.size())) {
            int count = functionCalls.size();
            String withoutTopLevel = concat(" ",
                    descriptor.parentWithoutTopLevelCommand, descriptor.commandName);
            functionCalls.add(format(
                    "  if CompWordsContainsArray \"${cmds%2$d[@]}\"; "
                            + "then %1$s; return $?; fi\n",
                    descriptor.functionName, count));
            buff.append(format("  local cmds%2$d=(%1$s)\n", withoutTopLevel, count));
        }
        buff.append("\n");
        Collections.reverse(functionCalls);
        for (String fc : functionCalls) {
            buff.append(fc);
        }

        buff.append(format(
                "\n  # No subcommands were specified; generate completions for "
                        + "the top-level command.\n"
                        + "  _picocli_%1$s; return $?;\n"
                        + "}\n",
                scriptName));
        return buff.toString();
    }

    // ---- per-command function ----

    private static String generateFunctionForCommand(String functionName,
                                                      String commandName,
                                                      CommandSpec spec) {
        // Classify options as flag (boolean) vs arg (takes a value)
        List<OptionSpec> flagOptions = new ArrayList<>();
        List<OptionSpec> argOptions = new ArrayList<>();
        for (OptionSpec opt : spec.options()) {
            if (opt.hidden()) {
                continue;
            }
            if (opt.isBoolean()) {
                flagOptions.add(opt);
            } else {
                argOptions.add(opt);
            }
        }

        String flagOptionNames = optionNames(flagOptions);
        String argOptionNames = optionNames(argOptions);

        // Visible subcommand names
        Set<String> subCmds = new LinkedHashSet<>();
        for (String sub : spec.subcommands().keySet()) {
            subCmds.add(sub);
        }
        String commands = String.join(" ", subCmds).trim();

        StringBuilder buff = new StringBuilder(1024);
        String sub = functionName.equals("_picocli_" + commandName) ? "" : "sub";
        String previousWord = argOptions.isEmpty()
                ? "" : "  local prev_word=${COMP_WORDS[COMP_CWORD-1]}\n";
        buff.append(format(
                "\n# Generates completions for the options and subcommands of `%s` %scommand.\n"
                        + "function %s() {\n"
                        + "  # Get completion data\n"
                        + "  local curr_word=${COMP_WORDS[COMP_CWORD]}\n"
                        + "%s"
                        + "\n"
                        + "  local commands=\"%s\"\n"
                        + "  local flag_opts=\"%s\"\n"
                        + "  local arg_opts=\"%s\"\n",
                commandName, sub, functionName, previousWord,
                commands, flagOptionNames, argOptionNames));

        // case statement for arg-taking options
        buff.append(generateOptionsSwitch(argOptions, spec));

        // positional param support
        String paramsCases = generatePositionalParamsCases(spec.parameters(), commandName);

        String posParamsFooter = "";
        if (!paramsCases.isEmpty()) {
            posParamsFooter = format(
                    "    local currIndex\n"
                            + "    currIndex=$(currentPositionalIndex \"%s\" "
                            + "\"${arg_opts}\" \"${flag_opts}\")\n"
                            + "%s",
                    commandName, paramsCases);
        }
        buff.append(format(
                "\n"
                        + "  if [[ \"${curr_word}\" == -* ]]; then\n"
                        + "    COMPREPLY=( $(compgen -W \"${flag_opts} ${arg_opts}\" "
                        + "-- \"${curr_word}\") )\n"
                        + "  else\n"
                        + "    local positionals=\"\"\n"
                        + "%s"
                        + "    local IFS=$'\\n'\n"
                        + "    COMPREPLY=( $(compgen -W "
                        + "\"${commands// /$'\\n'}${IFS}${positionals}\" "
                        + "-- \"${curr_word}\") )\n"
                        + "  fi\n"
                        + "}\n",
                posParamsFooter));
        return buff.toString();
    }

    private static String generateOptionsSwitch(List<OptionSpec> argOptions,
                                                 CommandSpec spec) {
        if (argOptions.isEmpty()) {
            return "";
        }
        StringBuilder cases = new StringBuilder();
        for (OptionSpec option : argOptions) {
            if (option.hidden()) {
                continue;
            }
            Class<?> type = option.type();
            String caseLabel = singleQuoteJoin(option.names());
            if (isFileType(type)) {
                cases.append(format("    %s)\n", caseLabel));
                cases.append("      local IFS=$'\\n'\n");
                cases.append("      type compopt &>/dev/null && compopt -o filenames\n");
                cases.append("      COMPREPLY=( $( compgen -f -- \"${curr_word}\" ) ) # files\n");
                cases.append("      return $?\n");
                cases.append("      ;;\n");
            } else {
                cases.append(format("    %s)\n", caseLabel));
                cases.append("      return\n");
                cases.append("      ;;\n");
            }
        }
        if (cases.isEmpty()) {
            return "";
        }
        return "\n"
                + "  type compopt &>/dev/null && compopt +o default\n\n"
                + "  case ${prev_word} in\n"
                + cases
                + "  esac\n";
    }

    private static String generatePositionalParamsCases(List<ParameterSpec> params,
                                                         String commandName) {
        StringBuilder buff = new StringBuilder();
        for (ParameterSpec param : params) {
            if (param.hidden()) {
                continue;
            }
            Class<?> type = param.type();
            if (param.isMultiValue()) {
                // for List<File>, the element type is likely String — skip special handling
            }
            if (isFileType(type)) {
                String ifOrElif = buff.isEmpty() ? "if" : "elif";
                int idx = param.index();
                buff.append(format("    %s (( currIndex >= %d )); then\n", ifOrElif, idx));
                buff.append("      local IFS=$'\\n'\n");
                buff.append("      type compopt &>/dev/null && compopt -o filenames\n");
                buff.append("      positionals=$( compgen -f -- \"${curr_word}\" ) # files\n");
            }
        }
        if (!buff.isEmpty()) {
            buff.append("    fi\n");
        }
        return buff.toString();
    }

    // ---- helpers ----

    private static boolean isFileType(Class<?> type) {
        return java.io.File.class.equals(type)
                || "java.nio.file.Path".equals(type.getName());
    }

    private static String optionNames(List<OptionSpec> options) {
        StringBuilder sb = new StringBuilder();
        for (OptionSpec option : options) {
            if (option.hidden()) {
                continue;
            }
            for (String name : option.names()) {
                if (!sb.isEmpty()) {
                    sb.append(' ');
                }
                sb.append('\'').append(name).append('\'');
            }
        }
        return sb.toString();
    }

    private static String singleQuoteJoin(String[] names) {
        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            if (!sb.isEmpty()) {
                sb.append('|');
            }
            sb.append('\'').append(name).append('\'');
        }
        return sb.toString();
    }

    private static String concat(String infix, String... values) {
        StringBuilder sb = new StringBuilder();
        for (String val : values) {
            if (val == null || val.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(infix);
            }
            sb.append(val);
        }
        return sb.toString();
    }

    // ---- script templates ----

    private static final String SCRIPT_HEADER = ""
            + "#!/usr/bin/env bash\n"
            + "#\n"
            + "# %1$s Bash Completion\n"
            + "# =======================\n"
            + "#\n"
            + "# Bash completion support for the `%1$s` command,\n"
            + "# generated by [QuickCLI](https://quarkus.io/) version %2$s.\n"
            + "#\n"
            + "# Installation\n"
            + "# ------------\n"
            + "#\n"
            + "# 1. Source all completion scripts in your .bash_profile\n"
            + "#\n"
            + "#   cd $YOUR_APP_HOME/bin\n"
            + "#   for f in $(find . -name \"*_completion\"); do line=\". $(pwd)/$f\"; "
            + "grep \"$line\" ~/.bash_profile || echo \"$line\" >> ~/.bash_profile; done\n"
            + "#\n"
            + "# 2. Open a new bash console, and type `%1$s [TAB][TAB]`\n"
            + "#\n"
            + "# 1a. Alternatively, if you have bash-completion installed:\n"
            + "#     Place this file in a `bash-completion.d` folder:\n"
            + "#\n"
            + "#   * /etc/bash-completion.d\n"
            + "#   * /usr/local/etc/bash-completion.d\n"
            + "#   * ~/bash-completion.d\n"
            + "#\n"
            + "\n"
            + "if [ -n \"$BASH_VERSION\" ]; then\n"
            + "  # Enable programmable completion facilities when using bash\n"
            + "  shopt -s progcomp\n"
            + "elif [ -n \"$ZSH_VERSION\" ]; then\n"
            + "  # Make alias a distinct command for completion purposes when using zsh\n"
            + "  setopt COMPLETE_ALIASES\n"
            + "  alias compopt=complete\n"
            + "\n"
            + "  # Enable bash completion in zsh\n"
            + "  if ! type compdef > /dev/null; then\n"
            + "    autoload -U +X compinit && compinit\n"
            + "  fi\n"
            + "  autoload -U +X bashcompinit && bashcompinit\n"
            + "fi\n"
            + "\n"
            + "# CompWordsContainsArray takes an array and then checks\n"
            + "# if all elements of this array are in the global COMP_WORDS array.\n"
            + "#\n"
            + "# Returns zero (no error) if all elements of the array are in the "
            + "COMP_WORDS array,\n"
            + "# otherwise returns 1 (error).\n"
            + "function CompWordsContainsArray() {\n"
            + "  declare -a localArray\n"
            + "  localArray=(\"$@\")\n"
            + "  local findme\n"
            + "  for findme in \"${localArray[@]}\"; do\n"
            + "    if ElementNotInCompWords \"$findme\"; then return 1; fi\n"
            + "  done\n"
            + "  return 0\n"
            + "}\n"
            + "function ElementNotInCompWords() {\n"
            + "  local findme=\"$1\"\n"
            + "  local element\n"
            + "  for element in \"${COMP_WORDS[@]}\"; do\n"
            + "    if [[ \"$findme\" = \"$element\" ]]; then return 1; fi\n"
            + "  done\n"
            + "  return 0\n"
            + "}\n"
            + "\n"
            + "# The `currentPositionalIndex` function calculates the index of the "
            + "current positional parameter.\n"
            + "#\n"
            + "# currentPositionalIndex takes three parameters:\n"
            + "# the command name,\n"
            + "# a space-separated string with the names of options that take a "
            + "parameter, and\n"
            + "# a space-separated string with the names of boolean options "
            + "(that don't take any params).\n"
            + "# When done, this function echos the current positional index to "
            + "std_out.\n"
            + "#\n"
            + "# Example usage:\n"
            + "# local currIndex=$(currentPositionalIndex \"mysubcommand\" "
            + "\"$ARG_OPTS\" \"$FLAG_OPTS\")\n"
            + "function currentPositionalIndex() {\n"
            + "  local commandName=\"$1\"\n"
            + "  local optionsWithArgs=\"$2\"\n"
            + "  local booleanOptions=\"$3\"\n"
            + "  local previousWord\n"
            + "  local result=0\n"
            + "\n"
            + "  for i in $(seq $((COMP_CWORD - 1)) -1 0); do\n"
            + "    previousWord=${COMP_WORDS[i]}\n"
            + "    if [ \"${previousWord}\" = \"$commandName\" ]; then\n"
            + "      break\n"
            + "    fi\n"
            + "    if [[ \"${optionsWithArgs}\" =~ ${previousWord} ]]; then\n"
            + "      ((result-=2)) # Arg option and its value not counted as "
            + "positional param\n"
            + "    elif [[ \"${booleanOptions}\" =~ ${previousWord} ]]; then\n"
            + "      ((result-=1)) # Flag option itself not counted as positional "
            + "param\n"
            + "    fi\n"
            + "    ((result++))\n"
            + "  done\n"
            + "  echo \"$result\"\n"
            + "}\n"
            + "\n"
            + "# compReplyArray generates a list of completion suggestions based on "
            + "an array, ensuring all values are properly escaped.\n"
            + "#\n"
            + "# compReplyArray takes a single parameter: the array of options to be "
            + "displayed\n"
            + "#\n"
            + "# The output is echoed to std_out, one option per line.\n"
            + "#\n"
            + "# Example usage:\n"
            + "# local options=(\"foo\", \"bar\", \"baz\")\n"
            + "# local IFS=$'\\n'\n"
            + "# COMPREPLY=($(compReplyArray \"${options[@]}\"))\n"
            + "function compReplyArray() {\n"
            + "  declare -a options\n"
            + "  options=(\"$@\")\n"
            + "  local curr_word=${COMP_WORDS[COMP_CWORD]}\n"
            + "  local i\n"
            + "  local quoted\n"
            + "  local optionList=()\n"
            + "\n"
            + "  for (( i=0; i<${#options[@]}; i++ )); do\n"
            + "    # Double escape, since we want escaped values, but compgen -W "
            + "expands the argument\n"
            + "    printf -v quoted %%q \"${options[i]}\"\n"
            + "    quoted=\\'${quoted//\\'/\\'\\\\\\'\\'}\\'\n"
            + "\n"
            + "    optionList[i]=$quoted\n"
            + "  done\n"
            + "\n"
            + "  # We also have to add another round of escaping to $curr_word.\n"
            + "  curr_word=${curr_word//\\\\/\\\\\\\\}\n"
            + "  curr_word=${curr_word//\\'/\\\\\\'}\n"
            + "\n"
            + "  # Actually generate completions.\n"
            + "  local IFS=$'\\n'\n"
            + "  echo -e \"$(compgen -W \"${optionList[*]}\" -- \"$curr_word\")\"\n"
            + "}\n"
            + "\n";

    private static final String SCRIPT_FOOTER = ""
            + "\n"
            + "# Define a completion specification (a compspec) for the\n"
            + "# `%1$s`, `%1$s.sh`, and `%1$s.bash` commands.\n"
            + "# Uses the bash `complete` builtin to specify that shell function\n"
            + "# `_complete_%1$s` is responsible for generating possible completions "
            + "for the\n"
            + "# current word on the command line.\n"
            + "# The `-o default` option means that if the function generated no "
            + "matches, the\n"
            + "# default Bash completions and the Readline default filename "
            + "completions are performed.\n"
            + "complete -F _complete_%1$s -o default %1$s %1$s.sh %1$s.bash\n";
}
