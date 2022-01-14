rootProject.name = "My website (reisishot.pictures)"
// Utils
include("commons")
include("image-access")
//Split up backend dependencies
include("backend:config")
include("backend:website-config")
include("backend:html")
include("backend:root")
include("backend:generators:gallery-abstract")
include("backend:generators:page")
include("backend:generators:page:keyword")
include("backend:generators:page:minimal")
include("backend:generators:page:overview")
include("backend:generators:link")
include("backend:generators:testimonial")
include("backend:generators:sitemap")
include("backend:generators:gallery")
include("backend:generators:thumbnail-abstract")
include("backend:generators:thumbnail-imagick")
include("backend:generators:multisite")
// Projects (depend on utils but not on each other)
include("backend")
include("backend:runner")
include("mise-utils")
// Meta Subproject for all uis
include("ui")
include("ui:base-ui")
include("ui:config-ui")
include("ui:exif-ui")


