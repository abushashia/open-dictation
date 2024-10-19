package com.sitedictation;

import java.io.InputStream;
import java.util.Collection;

interface TransactionImporterHelper {

    Collection<String> getFileNames();

    InputStream getInputStream(String fileName);
}
