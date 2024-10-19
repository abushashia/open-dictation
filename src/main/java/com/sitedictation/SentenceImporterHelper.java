package com.sitedictation;

import java.io.InputStream;
import java.util.Collection;

interface SentenceImporterHelper {

    Collection<String> getCorpora();

    InputStream getInputStream(String corpus);
}
