(ns igpop.profile.extension-dict)

(defn id-extension
  [field-name]
  {:id             (str "Extension.extension:" field-name ".id"),
   :path           "Extension.extension.id",
   :representation ["xmlAttr"],
   :short          "Unique id for inter-element referencing",
   :definition     "Unique id for the element within a resource (for internal references). This may be any string value that does not contain spaces.",
   :min            0,
   :max            "1",
   :base           {:path "Element.id",
                    :min  0,
                    :max  "1"},
   :type           [{:extension [{:url      "http//hl7.org/fhir/StructureDefinition/structuredefinition-fhir-type",
                                  :valueUrl "string"}],
                     :code      "http//hl7.org/fhirpath/System.String"}],
   :isModifier     false,
   :isSummary      false,
   :mapping        {identity "rim",
                    map      "n/a"}})


(defn extension-extension
  [field-name]
  {:id         (str "Extension.extension:" field-name ".extension"),
   :path       "Extension.extension.extension",
   :slicing    {:discriminator [{:type "value",
                                 :path "url"}],
                :description   "Extensions are always sliced by (at least) url",
                :rules         "open"},
   :short      "Additional content defined by implementations",
   :definition "May be used to represent additional information that is not part of the basic definition of the element. To make the use of extensions safe and manageable, there is a strict set of governance  applied to the definition and use of extensions. Though any implementer can define an extension, there is a set of requirements that SHALL be met as part of the definition of the extension.",
   :comment    "There can be no stigma associated with the use of extensions by any application, project, or standard - regardless of the institution or jurisdiction that uses or defines the extensions.  The use of extensions is what allows the FHIR specification to retain a core level of simplicity for everyone.",
   :alias      ["extensions",
                "user content"],
   :min        0,
   :max        "*",
   :base       {:path "Element.extension",
                :min  0,
                :max  "*"},
   :type       [{:code "Extension"}],
   :constraint [{:key        "ele-1",
                 :severity   "error",
                 :human      "All FHIR elements must have a @value or children",
                 :expression "hasValue() or (children().count() > id.count())",
                 :xpath      "@value|f:*|h:div",
                 :source     "http://hl7.org/fhir/StructureDefinition/Element"},
                {:key        "ext-1",
                 :severity   "error",
                 :human      "Must have either extensions or value[x], not both",
                 :expression "extension.exists() != value.exists()",
                 :xpath      "exists(f:extension)!=exists(f:*[starts-with(local-name(.), \"value\")])",
                 :source     "http://hl7.org/fhir/StructureDefinition/Extension"}],
   :isModifier false,
   :isSummary  false,
   :mapping    [{:identity "rim",
                 :map      "n/a"}]})

(defn base-extension [field-name path]

  {:id          path,
   :path        path,
   :sliceName   field-name,
   :min         0,
   :max         "1",
   :type        [{:code    "Extension",
                  :profile [(str "http://hl7.org/fhir/us/core/StructureDefinition/" name)]}],
   :mustSupport true,
   :mapping     [{:map field-name}]})

(defn url-extension [field-name path]

  {:id       (str "Extension.extension:" field-name ".url"),
   :path     "Extension.extension.url",
   :min      1,
   :max      "1",
   :type     [{:code "uri"}],
   :fixedUri "ombCategory"})

(defn value-extension
  [id path type required description]
  {
   :id      (str path ".value" type),
   :path    (str path ".value" type),
   :min     1,
   :max     "1",
   :type    [{:code type}],
   :binding {:strength    required,
             :description description,
             :valueSet    "http://hl7.org/fhir/us/core/ValueSet/omb-race-category"}})