# Lollipop is Sugar FHIR Profiling

With this project we want to simplify definition of FHIR profiles
with Data DSL, which can be maintained manually and pushed into source
code repository. FHIR StructureDefinitions and other resources can be generated
from this DSL. It should be DRY, Convetion Over Configuration and 100% pragmatic.

IG generator is comming as part of this utils.


### Features

* in place extension and valueset definitions
* validators in multiple languages 
* first class extensiosn 


### TODO

* generate FHIR profiles
* validate profiles
* best practice warnings
* generate docs
* generate validators

## Basic structure

Profile definition is a yaml document. 

```yaml
id: us-core-patient
resourceType: Patient
desc: |
  Information about an individual or animal receiving health care services
  
# constraint FHIR elements and define extensions
attrs:
  race:
    ext: http://hl7.org/fhir/us/core/StructureDefinition/us-core-race
    mustSupport: true
    desc: Patient race
  ethnicity:
    ext: http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity
    mustSupport: true
    desc: Patient ethnicity


```


## Profiling



### Changing Cardinality

Restricting the cardinality of the element; e.g. the base might allow 0..*,
and a particular application might support 1..2

To overcome XML legacy in FHIR we took more JSON oriented view of FHIR resources
and distinguish collections from singular elements. So we use `isRequired` to mark singular
elements required (i.e. 1..1 in FHIR). We use `min/max` to constraint collections. If `isRequired` is set on
collection - this means `at least one element is present`.


```yaml
attrs:
  birthDate:
    type: date
    isRequired: true
  name:
    isRequired: true
  identifier:
    min: 1 # same as isRequired: true
    max: 10 # constraint max number of identifiers - bad idea
    isCollection: true

```

### Turn of element

Ruling out use of an element by setting its maximum cardinality to 0

```
attrs:
  animal:
    dontUse: true
```

### Documentation

Providing refined definitions, comments/usage notes and examples for the elements 
defined in a Resource to reflect the usage of the element within the context of the Profile

### Define must-support

Declaring that one or more elements in the structure must be 'supported' (see below)



### FHIRPath constraints + simplified dsl rules

Making additional constraints on the content of nested elements within the resource 
(expressed as XPath statements)

Add FHIRPath invariants


### Union Types

Restricting the types for an element that allows multiple types

### Typed Reference

Requiring a typed element or the target of a resource reference to conform to 
another structure profile (declared in the same profile, or elsewhere)

### ValueSet binding

Specifying a binding to a different terminology value set (see below)


### [-] Default Value

Restricting the contents of an element to a single fixed value

Implicit logic is not good! 
Resource modification is not good!


### [-] Mapping to other standards

Providing more specific or additional mappings (e.g. to HL7 v2  or HL7 v3 ) 
for the resource when used in a particular context

