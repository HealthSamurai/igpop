# igpop is Sugar FHIR Profiling

DRY FHIR profiling for programming humans

* 80/20 of profiling
* convention over configuration
* to be manually written
* Data DSL - no grammars and parsers
* inline extensions
* inline valuesets

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
  Observation/smoking-status.yaml <- smoking status profile for Observation
  vs.smoking-status.yaml <- valuset for smoking status
  vs.smoking-status.csv <- you can put concepts as a csv file
  ...
build/
  fhir/ <- SD, VS etc
  json-schema/ <- json-schema
  igpop/ <- igpop build
  site/  <- static site
ig.yaml <- manifest file
package.json
```


## Profiles

You create basic profile for resource type by 
creating `src/<resource-type>.yaml` file.

For example `src/Patient.yaml` is your basic Patient.
Id and url for generated structure definition will be deduced
from ig.yaml id and url by convetion.

Basic structure of your profile is:

```yaml
# profile description
description: ...
  
# constraints & extensions definitions
elements: ...

# REST API definitions
api: ...

# Examples section
examples: ...
```

### elements

Object which defines element selectors to put constraints or define extensions.
Complex elements can have nested `elements` definitions.


Each element can have keys:

* description - description
* type - primitive or complex type name
* valuset - binding to valueset
* elements - nested elements for complex elements
* constant - check for equality
* Collections:
  * maxItems & minItems - constraints for collection 
  * contains - rule wich check inclusion of pattern in collection
* Single elements
  * required - make element required
  * match - rule wich check inclusion of pattern

Example src/Patient.yaml:

```yaml
description: My Basic Patient Profile
elements:
  identifier: # []
    # require at least one element in collection
    minItems: 1
    elements:
      # require value in identifier
      value: { required: true }
      system:
        required: true
        # define binding and inline definition for valuset
        valueset:
          id: patient-systems
          strength: extensible
          system: local
          concepts:
          - {code: 'ssn'}
          - {code: 'driver-license}
  name:
    minItems: 1
    elements:
      family: { required: true }
      given:  { minItems: 1 }
  # if element listed without attributes - that means 'mustSupport' will be added
  birthDate: {}

```

## Constraint cardinality

igpop distinguishes collections and singular elements.

## Define ValueSet bindings

## Constant checks

## Slicing

## Define extensions 
