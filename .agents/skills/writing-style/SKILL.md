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

- One idea per sentence. Keep each sentence under 32 words.
- Active voice, present tense: "The step registers a service", not "a service
  is now being registered by the step".
- Use the verb, not a noun made from it: "the step registers", not "the
  registration is done by the step".
- Put the condition before the action: "If the datasource is inactive, the
  pool is empty".
- Do not join two clauses with a semicolon or a dash. Start a new sentence.
- Do not put an essential fact in parentheses.
- Avoid filler words and the terms the Quarkus Vale style warns about. The
  lists are in `docs/.vale/styles/Quarkus`, mainly `Fluff.yml` and the
  `Terms*.yml` files.

## Words

- One term per concept, used the same way in the text and in the code. Do not
  alternate between "service", "service value" and "registered value" for one
  thing.
- Spell out an abbreviation the first time, unless it is a project term
  (ArC, CDI, JTA).

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

Read the draft once more before you post it. Check that each concept keeps one
term, and that the reason comes before the details.
