# familie-ba-maler
Brevmaler for barnetrygd

Url dev: https://familie-ba-dokgen.dev-adeo.no/

Url prod: https://familie-ba-dokgen.adeo.no/

##Kjøring lokalt
Kjør `up.sh`, eller `docker-compose up -d`.

Merk at docker-compose-konfigen lar deg oppdatere templates uten å restarte containere.

##Quirks/kompilering
dokgen sliter med whitespace i hbs-templates, spesielt indentering. [Ref slack](https://nav-it.slack.com/archives/CJN0STWB0/p1619430956095800)

For å løse dette kan man skrive templates med ønsket indentering under `content/templates/{prosjekt}/uncompiled/{template}.hbs`,
og så compile de med `compiler.py`. Output legges i filer med samme navn i mappen over `uncompiled`.
Compileren krever at watchdog-pakken er installert i python-miljøet.
En pipenv-fil følger også med for å forenkle oppsett av kompilering.
```shell
$ pipenv install
$ pipenv run ./compiler.py
```