## igpop-ui

Problem: FHIR profiling (tool)
Solution: igpop

Problem: Effective FHIR profiling
Solution: igpop IDE integration

* igpop LSP
* igpop 
  * vscode
  * emacs
  * web (monaco + lsp client)
    + IG page edit link
    + open/safe igpop server
    + redirect back


## Features:

* autocomplete - fast & less errors
  * FHIR elements (complex types)
  * igpop keys
  + valuset

* documentation
  * FHIR & igpop

* diagnostics
  * syntax
  * collection/single element rules (maxItems could not be applied to single element)
  * unknown keys
  * unknown valueset ids


### Extra

* build and publish of igpop as standard vscode extension
* create new profile
* result in ide (SD, IG page)
* fhirpath support

## Demo

* vscode show features
* web (monaco) - deploy as container
* emacs


## Technical probles:

* parser
* json-rpc 
  * websockets
  * stdout
