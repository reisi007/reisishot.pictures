rootProject.name = "My website (reisishot.pictures)"
// Utils
include("commons")
include("image-access")
include("backend-config")
// Projects (depend on utils but not on each other)
include("backend")
include("mise-utils")
// No more needed include("piwigo-converter")
include("hocon2json")
// Meta Subproject for all uis
include("ui")
include("ui:base-ui")
include("ui:config-ui")
include("ui:exif-ui")


