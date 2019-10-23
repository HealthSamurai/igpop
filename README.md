# igpop is Sugar FHIR Profiling

DRY FHIR profiling for programming humans

* 80/20 of profiling
* convention over configuration
* to be manually written
* Data DSL - no grammars and parsers

### Features

* generate FHIR StructureDefinition, ValueSets etc
* generate IG static site
* generate json schema
* linter for profiles

## New Project

Install igpop util and start your project

```
src/
  Patient.yaml <- basic profile for Patient
  Observation.smoking-status.yaml <- smoking status profile for Observation
  valuset.smoking-status.yaml <- valuset for smoking status
  ...
build/
  fhir/ <- SD, VS etc
  json-schema/ <- json-schema
  igpop/ <- igpop build
  site/  <- static site
IG.yaml <- manifest file
package.json
```


## Basic structure

Profile definition is a yaml document. 
It consist of keys with it's onw semantic

* desc - description
* elements - list of elements
* api - API profiling
* example - collection of examples


File: us-core/Patient.yaml defines us-core-patient basic profile

```yaml
description: |
  Information about an individual or animal receiving health care services
  
# constraint FHIR elements and define extensions
elements: ...

# REST API definition
api: ...

# collection of examples
examples: ...
```


### desc

Text notes about profile or element. Can be in markdown format.

### elements

Key-Value object which defines or constraint elements.

Collection elements are posfixed with `[]`.

Complex elements can have nested `elements` definitions.


Each element can have keys:

* description - description
* type - primitive or complex type name
* required - make element required
* maxItems & minItems - constraints for collection 
* valuset - binding to valueset
* contains - rule wich check inclusion of pattern in collection
* match - rule wich check inclusion of pattern
* elements - nested elements for complex elements

Example:

```yaml
id: Patient
description: Basic FHIR Patient Profile
elements:
  identifier[]:
    minItems: 1
    elements:
      value: { required: true }
      system:
        valueset:
          id: patient-systems
          concepts:
          - {code: 'ssn'}
          - {code: 'driver-license}
  name[]:
    minItems: 1
    elements:
      family: { required: true }
      given[]: { minItems: 1 }

```
