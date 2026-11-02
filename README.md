# structurizr-mermaid-plugin

Structurizr DSL plugin that replaces ```` ```mermaid ```` blocks in documentation (`!docs`) with an image from a Mermaid renderer you host yourself (the mermaid.ink URL scheme, e.g. the `mermaid.ink` container). Nothing is sent to a third party.

```
mvn package          # target/structurizr-mermaid-1.0.0.jar
```

Put the jar in a `plugins/` directory next to the workspace DSL, then, after the `!docs` statements:

```
!plugin bata.structurizr.MermaidPlugin {
    url http://localhost:8888
    format svg
    theme neutral
}
```

`url` is required; `format` (svg or png) and `theme` are optional. Built against `structurizr-dsl` 4.1.0, Java 17+.

## Licence

Apache-2.0. Copyright 2026 Joris Wijlens.
