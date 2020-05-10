# Changelog

This log documents significant changes for each release.
This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

- LSP linter to console
- Profile examples in a separate folder
- Valuesets preview
- References to manifest elements ```{{project.title}}```
- File name and ids ```{{project.id}}-{{lowercase(rt)}}```
- Blockquotes for markdown docs 

## [Draft]

## [0.3.0] - 2020-05-06
### Added
- Exists and union(type) discriminators to slices
- Links to external valuesets by :url key

### Changed
- Overriding slice item elements with it's own nested elements

### Fixed
- Valueset references in profiles
- Empty element type bug

## [0.2.0] - 2020-04-06
### Added
- The ability to create custom columns in valuesets

### Changed
- Docs, profiles and valuesets menu links lead to the first items of the each section instead of dashboards

### Fixed
- Docs menu items titles
- Docs menu items order

## [0.1.0] - 2020-02-17
### Added
- Monaco for dev

### Changed
- homepage.md moved to /src

## [0.0.4] - 2020-02-04
### Added
 - Extensions
 - Slices for constant and match
 - URL, constant, valueset strength in element description
 - Icons for slices, extensions, complex extensions
 - UI home link
 
### Changed
 - Slices in spec
 
### Fixed
 - Icon for reference type

## [0.0.3] - 2020-01-24
### Added
 - Snapshot generation
 - Tabs for differential, snapshot, examples and resource content
 - Homepage via separated file
 - Nested docs
 - Docs dashboard
 
### Fixed
 - Base profile view

## [0.0.2] - 2020-01-18
### Added
 - Extensions in spec

### Changed
 - Publishing releases in npm instead of github
 - Running igpop by script "igpop"

### Fixed
 - Paths to igpop-fhir-4.0.0 resources
 - System for single concept in valuesets
 - Spec

## [0.0.1] - 2019-12-16
### Added
 - This is a FHIR profiling tool see more in [README](https://github.com/HealthSamurai/igpop/blob/master/README.md) and [spec](https://github.com/HealthSamurai/igpop/blob/master/igpop.md)
 - This CHANGELOG file
 - Deploying to Github
 
### Fixed
 - tables view in markdown files
 - igpop dev {port_name} command changed to igpop dev -p {port_name}
