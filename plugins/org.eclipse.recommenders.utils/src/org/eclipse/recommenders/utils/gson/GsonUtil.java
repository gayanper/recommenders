/**
 * Copyright (c) 2010 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.recommenders.utils.gson;

import static java.util.Objects.requireNonNull;
import static org.eclipse.recommenders.utils.Throws.throwUnhandledException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.recommenders.utils.IOUtils;
import org.eclipse.recommenders.utils.names.IFieldName;
import org.eclipse.recommenders.utils.names.IMethodName;
import org.eclipse.recommenders.utils.names.IPackageName;
import org.eclipse.recommenders.utils.names.ITypeName;
import org.eclipse.recommenders.utils.names.VmFieldName;
import org.eclipse.recommenders.utils.names.VmMethodName;
import org.eclipse.recommenders.utils.names.VmPackageName;
import org.eclipse.recommenders.utils.names.VmTypeName;

import com.google.common.collect.Multimap;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class GsonUtil {

    public static final Type T_LIST_STRING = new TypeToken<List<String>>() {
    }.getType();

    private static Gson gson;

    public static synchronized Gson getInstance() {
        if (gson == null) {
            final GsonBuilder builder = new GsonBuilder();

            builder.registerTypeAdapter(VmMethodName.class, new MethodNameTypeAdapter());
            builder.registerTypeAdapter(IMethodName.class, new MethodNameTypeAdapter());

            builder.registerTypeAdapter(VmTypeName.class, new TypeNameTypeAdapter());
            builder.registerTypeAdapter(ITypeName.class, new TypeNameTypeAdapter());

            builder.registerTypeAdapter(VmFieldName.class, new FieldNameTypeAdapter());
            builder.registerTypeAdapter(IFieldName.class, new FieldNameTypeAdapter());

            builder.registerTypeAdapter(VmPackageName.class, new PackageNameTypeAdapter());
            builder.registerTypeAdapter(IPackageName.class, new PackageNameTypeAdapter());

            builder.registerTypeAdapter(File.class, new FileTypeAdapter());

            builder.registerTypeAdapter(UUID.class, new UuidTypeAdapter());

            builder.registerTypeAdapter(Date.class, new ISO8601DateParser());

            builder.registerTypeAdapter(Multimap.class, new MultimapTypeAdapter());

            builder.registerTypeAdapterFactory(new MultisetTypeAdapterFactory());

            builder.enableComplexMapKeySerialization();
            builder.setPrettyPrinting();
            gson = builder.create();
        }
        return gson;
    }

    public static <T> T deserialize(final CharSequence json, final Type classOfT) {
        return deserialize(json.toString(), classOfT);
    }

    public static <T> T deserialize(final String json, final Type classOfT) {
        requireNonNull(json);
        requireNonNull(classOfT);
        try {
            return getInstance().fromJson(json, classOfT);
        } catch (final Exception e) {
            throw throwUnhandledException(e);
        }
    }

    public static <T> T deserialize(final File jsonFile, final Type classOfT) {
        requireNonNull(jsonFile);
        requireNonNull(classOfT);
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(jsonFile));
            return deserialize(in, classOfT);
        } catch (final Exception e) {
            throw throwUnhandledException(e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public static <T> T deserialize(final InputStream jsonStream, final Type classOfT) {
        requireNonNull(jsonStream);
        requireNonNull(classOfT);
        Reader reader = null;
        try {
            reader = new InputStreamReader(jsonStream, StandardCharsets.UTF_8);
            return getInstance().fromJson(reader, classOfT);
        } catch (final Exception e) {
            throw throwUnhandledException(e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public static String serialize(final Object obj) {
        requireNonNull(obj);
        final StringBuilder sb = new StringBuilder();
        serialize(obj, sb);
        return sb.toString();
    }

    public static void serialize(final Object obj, final Appendable writer) {
        requireNonNull(obj);
        requireNonNull(writer);
        try {
            getInstance().toJson(obj, writer);
        } catch (final Exception e) {
            throw throwUnhandledException(e);
        }
    }

    public static void serialize(final Object obj, final File jsonFile) {
        requireNonNull(obj);
        requireNonNull(jsonFile);
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(jsonFile));
            serialize(obj, out);
        } catch (final Exception e) {
            throw throwUnhandledException(e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    public static void serialize(final Object obj, final OutputStream out) {
        requireNonNull(obj);
        requireNonNull(out);
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            getInstance().toJson(obj, writer);
        } catch (final Exception e) {
            throw throwUnhandledException(e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    public static <T> List<T> deserializeZip(File zip, Class<T> classOfT) throws IOException {
        List<T> res = new LinkedList<>();
        ZipInputStream zis = null;
        try {
            InputSupplier<FileInputStream> fis = Files.newInputStreamSupplier(zip);
            zis = new ZipInputStream(fis.getInput());
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    res.add(GsonUtil.<T>deserialize(zis, classOfT));
                }
            }
        } finally {
            Closeables.close(zis, true);
        }
        return res;
    }
}
