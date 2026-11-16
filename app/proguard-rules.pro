# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# ---------------------------------------------------------------------------
# Gson: classes (de)serialized by reflection. Field names are the JSON keys,
# so they must not be renamed or removed.
# ---------------------------------------------------------------------------
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Backup export (CalendarEvent -> JSON) and import (BackupEvent <- JSON)
-keep class danilovl.calendar.data.local.CalendarEvent { <fields>; <init>(...); }
-keep class danilovl.calendar.util.EventBackupUtils$BackupEvent { <fields>; <init>(...); }

# Holidays: nager.date API DTOs and bundled assets/holidays.json
-keep class danilovl.calendar.data.remote.HolidayDto { <fields>; <init>(...); }
-keep class danilovl.calendar.data.remote.CountryDto { <fields>; <init>(...); }
-keep class danilovl.calendar.data.repository.HolidayJson { <fields>; <init>(...); }

# Anonymous TypeToken subclasses carry the generic type used by Gson.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Readable crash stack traces in release.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
