---
name: writing-style
description: >
  How to write the text around a change: pull request descriptions, issue
  reports, commit messages, review replies, Javadoc and code comments.
---

# Writing Style

Reviewers skim. Every text around a change must be easy to scan and hard to
misread. These rules apply to pull request descriptions, issue reports, commit
messages, review comments and replies, Javadoc and code comments.

A template or another skill decides the structure, and this skill decides the
sentences inside it. The issue templates come first. So do the `pull-requests`
skill for titles and descriptions, the `coding-style` skill for where comments
belong, and the documentation style guide for `.adoc` files.

## Sentences

- One idea per sentence. Keep a sentence to about 20 words.
- Active voice, present tense: "The step registers a service", not "a service
  is now being registered by the step".
- Use the verb, not a noun made from it: "the step registers", not "the
  registration is done by the step".
- Put the condition before the action: "If the datasource is inactive, the
  pool is empty".
- Do not join two clauses with a semicolon or a dash. Start a new sentence.
- Do not put an essential fact in parentheses.
- Avoid filler words: currently, basically, simply, just, appropriate, various,
  "in order to", "note that", "etc.".

## Words

- One term per concept, used the same way in the text and in the code. Do not
  alternate between "service", "service value" and "registered value" for one
  thing.
- Spell out an abbreviation the first time, unless it is a project term
  (ArC, CDI, JTA).
- Prefer the plain word: "use", not "utilize" or "leverage"; "do", not
  "perform"; "make sure", not "ensure".

## Paragraphs and structure

- A paragraph has one topic and at most six sentences. The first sentence
  states the topic.
- Lead with the change and its reason. Details, test results and references
  come after.
- Use a list for parallel items and a table for numbers.
- Write a reference as a bare `#1234` so GitHub shows its title.

## Per kind of text

- **Pull request description**: why the change is needed, what it does, how
  you verified it, then `Fixes #n`. See the `pull-requests` skill.
- **Issue report**: fill the template fields in order. "Describe the bug" says
  what happens in one or two sentences. "How to reproduce" is a numbered list
  of steps a stranger can follow.
- **Commit message**: a one-sentence subject, a blank line, then a body in the
  same style as the description.
- **Review reply**: answer the question in the first sentence, then the commit
  link, then the evidence.
- **Javadoc**: one sentence that says what the member does or returns, in the
  imperative for methods ("Get the bean name."), then `@param` and `@return`.
- **Code comment**: one line that names what the block does. It never repeats
  the next statement.

## Checking a draft

`prose-lint.java` in this directory checks the mechanical rules. It reports
long sentences and paragraphs, passive voice, nominalizations, filler words,
parentheses, clause-joining dashes and semicolons, and noun clusters. It skips
code blocks, inline code, URLs, headings, tables and `Fixes #n` lines.

```bash
jbang .agents/skills/writing-style/prose-lint.java pr-description.md
# or, without JBang:
java .agents/skills/writing-style/prose-lint.java pr-description.md
```

The exit code is 1 when there is a finding. Treat a passive-voice finding as a
prompt to re-read, not as an error. "The PR is closed" is a state, not a
passive. The script cannot check that you use one term per concept, or that
the reason comes first. Those stay with the author.
