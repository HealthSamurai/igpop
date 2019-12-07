# igpop spec

## Project structure

```
docs/
  Intro.md - documentation pages
src/ - contains profiles and valueset definitions
  Patient.yaml - profile for patient
  Observation/
     bmi.yaml - specific profile for observation
  vs.gender.yaml - valueset definition
  vs.gender.csv  - codes for valueset
ig.yaml - manifest file
```

## ig.yaml

This is manifest file with project info

```yaml
elements:
  id: 
    type: string
    required: true
    description: Project ID, will be used as a prefix for all profiles and valueset ids
  fhir:
    type: code
    description: FHIR version for project
    enum: [3.0.1, 4.0.0]
  url:
    type: uri
    required: true
    description: Project URI
  title:
    type: string
    required: true
    description: Title of project for humans
  description:
    type: string
    description: Project description

```

You can refer manifest elements with `project.attr.attr` expression
in doc pages and profiles. For example:

file:docs/Intro.md
```
## {{project.title}}

This IG uses FHIR {{project.fhir}} version!
```


## File names and ids

File <resource-type>.yaml (for example Patient.yaml) defines
a only profile for <resource-type>.yaml

Id of this profile will be: `{{project.id}}-{{lowercase(rt)}}`.
For example for Patient.yaml and project.id = 'us-core' it will be `us-core-patient`.

Path <resource-type>/<id>.yaml defines named Profile. For example Observation/bmi.yaml.
Id of this profile will be: `{{project.id}}-{{lowercase(rt)}}-{{id}}` (for example: use-core-observation-bmi).

## Profile Schema


```yaml
description: IGPOP Profile definition
elements:
  elements:
    type: Map
    description: Definition of elements
    value:
      elements:
        type:  { type: code, description: Element type }
        description: { type: string, description: 'Element description'}
        elements: { ref: elements.elements, description: 'Nested elements'}
        required: { type: boolean }
        disabled: { type: boolean }
        collection: { type: boolean }
        minItems: { type: integer }
        maxItems: { type: integer }
        mustSupport: {type: boolen, default: true}
        valueset:
          elements:
             id: { type: 'string', description: 'valueset id' }
             url: { type: 'url' }
             strength: { type: 'code', enum: ['extensible', 'required'], default: 'extensible' }
        constant: { type: any }
        match: { type: any }
        refers:
          type: Map
          description: Reference specification
          key: { type: code, description: 'Resource type' }
          value: { type: string, description: 'Profile id' }
        constaints:
          type: Map
          description: FHIRPath rules, key is rule identifier
          value:
            elements:
              expression: { type: fhirpath }
              description: { type: string }
              severity:  { type: code, enum: ['error']}
        mapping:
          type: Map
          description: Mapping to other systems
          value:
            elements:
              map: { type: string }
        slices:
          type: Map
          key: {type: string, description: 'Slice Name'}
          description: Define named slices for collection
          value:
            elements:
               ref: 'elements.elements'
               match: { required: true }
        

```

## Profiling

### Cardinality

igpop differentiates collections and singular elements.
For singular elements like Patient.birthDate you have to use
`required` (min = 1) and `disabled` (max = 0) boolean flags insteand of FHIR `min/max`

```yaml
elements:
  birthDate: { required: true }
  animal: { disabled: true }

# FHIR

- path: Patient.birthDate
  max: 1
- path: Patient.animal
  max: 0

```

For collections you use `minItems`, `maxItems` and `disabled`

```yaml
elements:
  name: { minItems: 1, maxItems: 10 }
  communication: { disabled: true }

# FHIR

- path: Patient.name
  min: 1
  max: 10
- path: Patient.communication
  max: 0

```

###  Restricting the contents of an element

igpop uses 'constant' keyword to define fixed values

```yaml
elements:
  code: { constant: "female"}
  coding:
    constant:
       code: code-1
       system: sys-1

# FHIR

- path: RT.code
  fixedCode: 'female'
- path: RT.coding
  fixedCoding: 
    code: code-1
    system: sys-1

```

4. FHIRPath rules

FHIR:
```yaml
constraints:
- severity: error
  key: us-core-8
  expression: "family.exists() or given.exists()"
  human: "Patient.name.given or Patient.name.family or both SHALL be present"

```

IGPOP:
```yaml
elements:
  name:
    constaints:
      us-core-8: 
        expression: "family.exists() or given.exists()"
        description: "Patient.name.given or Patient.name.family or both SHALL be present"
        # severity: error (default)

```

5. Restricting polymorphic types

FHIR:
```yaml
---
path: Observation.value[x]
slicing:
  discriminator:
  - type: type
    path: "$this"
  ordered: false
  rules: closed
type:
- code: Quantity

```


IGPOP:

```yaml
# Observation
elements:
  value:
    Quantity: {}

```
6. Reference to profile

FHIR:
```yaml
---
path: Encounter.subject
type:
  - code: Reference
    targetProfile:
    - http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient

```

IGPOP
```yaml
elements:
  subject:
    refers: { Patient: # }

```

7. Binding to a value set 

FHIR:

```yaml
---
path: Encounter.participant.type
binding:
  strength: extensible
  description: Role of participant in encounter.
  valueSet: http://hl7.org/fhir/ValueSet/encounter-participant-type


```

```yaml
# Encounter.yaml
...
participant:
  elements:
    type: 
      description: Role of participant in encounter.
      valueset:  { id: encounter-participant-type } #default strength: extensible 

```

+ inline valueset
+ vs.<vs-id>.yaml
+ vs.<vs-id>.csv

8. Refined definitions, comments etc


```yaml
name:
  elements:
    given:
      # short in FHIR
      description: Имя и Отчество - первый и второй элементы соответственно 
      # comments: ...
      # definition: ...
      # requirements: ...

```

9. Providing more specific or additional mappings

FHIR:

```yaml
mapping:
- identity:
  language:
  map:
  comments:

```

IGPOP:
```yaml

name:
  mappings:
    hl7.v2: { map: PID-5, PID-9 }
    ru.tfoms: { map: XX-XX-F1 }

```

10. Must be 'supported' 

FHIR:
```yaml
path: Patient.name
mustSupport: true

```

IGPOP:
```yaml
elements:
   name: {} # mustSupport: true by default
   animal: { mustSupport: false }

```


## Extensions

FHIR: 
 extension definition
 slice in profile

IGPOP
```yaml

description:
  This profile sets minimum expectations for the Patient resource to record,
  search and fetch basic demographics and other administrative information about
  an individual patient. It identifies which core elements, extensions,
  vocabularies and value sets SHALL be present in the resource when using this
  profile.

elements:
  extension:
    race:
      description: US Core Race Extension
      elements:
        text: { required: true,  description: Race Text, type: string }
        ombCategory:
          collection: true
          minItems: 1
          type: Coding
          valueset: { id: omb-race-category }
        detailed:
          collection: true
          description: Extended race codes
          type: Coding
          valueset: { id: detailed-race }
    birthsex:
      type: code
      valueset: { id: birthsex }

```

## Slicing

Put constaints and extensions on collections:


1. value	

This is the most commonly used discriminator type: 
to decide based on the value of an element.
Elements used like this are mostly primitive types- code, uri.

FHIR: fixed[x] - IGPOP: constant

2. pattern	

This is mostly used with elements of type CodeableConcept
where the elements are distinguished by the presence of a particular code but other codes are expected to be present, and are irrelevant for the slice matching process.

FHIR: pattern[x] - IGPOP: match

3. type

Slice on polymorphic type or profiled references

4. profile 

Used to match slices based on the whether the item conforms to the specified profile. 

Composition.section.entry()


pattern nad fixed value

```yaml

# Patient.yaml
elements:
  identifier:
    descrption: Идентификаторы пациента такие как паспорт, СНИЛС и т.д.
    slices:
      pasport:
        match: { system: urn:fhir-ru:pasport }
        description: Паспортные данные
        required: true
        elements:
          value: { regexp: "XX-XX-XX" }
          extensions:
            serialNumber: { type: string }
      snils:
        match: { system: ..snils.. }
            

```

Built in slicing for polymorphics

```yaml
#Observation/.....yaml
elements:
  value:
    Quantity: {...}
    string: {...}

```
