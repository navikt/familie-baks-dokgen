# familie-baks-dokgen
Html-generator for barnetrygd- og kontantstøttesøknader

Importeres av familie-baks-mottak som en dependency for å generere html fra en av malene under [content/templates](content/templates) ved mottak av søknader, før konvertering til PDF for arkivering.

### Bygging lokalt
`mvn verify`

### Quirks/kompilering
dokgen sliter med whitespace i hbs-templates, spesielt indentering. [Ref slack](https://nav-it.slack.com/archives/CJN0STWB0/p1619430956095800)

For å løse dette kan man skrive templates med ønsket indentering under `content/templates/{prosjekt}/uncompiled/{template}.hbs`,
og så compile de med `compiler.py`.
En Pipfile følger med prosjektet som setter opp dependencies som trengs for å kjøre compiler.py. Tenk på denne som en package.json i et node-prosjekt.
Output legges i filer med samme navn i mappen over `uncompiled`.

```shell
$ # Installer pipenv først hvis det ikke er gjort fra før. Mac:
$ brew install pipenv
$ pipenv install
$ pipenv run ./compiler.py
```
