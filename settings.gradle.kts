rootProject.name = "My website (reisishot.pictures)"
// Utils
include("commons")
include("image-access")
//Split up backend dependencies
include("backend:testimonials")
include("backend:base")
include("backend:abstractGallery")
include("backend:config")
include("backend:generators:link")
include("backend:generators:sitemap")
include("backend:generators:gallery")
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


