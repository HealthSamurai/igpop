# Lollipop is Sugar FHIR Profiling

DRY FHIR profiling for hackers.

* Convention over Configuration.
* to be manually written
* Data DSL - no grammars and parsers

### Features

* generate StructureDefinition, ValueSets etc
* generate IG
* validate profiles
* linter for profiles

## Project Structure 


DRY: File path defines resource type, id and url of profile.
If in index.yaml we have `id: us-core`

Profile in pr/Patient.yaml has id = us-core-patient
VavlueSet in vs/race.yaml has id = us-core-valueset-race.yaml


```
us-core/
  core.yaml # package metadata
  Patient/
    attrs.yaml # basic profile for Patient
    vs/ # patient specific valusets
      race.yaml
      race.concepts.ndjson
  Practitioner.yaml # basic for practitioner
  Observation/
    lab.yaml # lab profile for Observation 
    smoking-status/
      core.yaml # another observation profile 
      vs/status-status.yaml
      ex/sm-1.yaml # example for observation
  vs/ # value sets
    shared.yaml

deps/ # dependencies
  fhir/vs/administrative-gender.yaml

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
