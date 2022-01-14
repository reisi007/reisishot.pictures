package pictures.reisishot.mise.backend

import at.reisishot.mise.commons.FilenameWithoutExtension

typealias ImageFetcher = (FilenameWithoutExtension) -> ImageInformation
