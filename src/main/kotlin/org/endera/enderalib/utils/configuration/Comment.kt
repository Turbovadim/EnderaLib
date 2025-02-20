package org.endera.enderalib.utils.configuration

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Comment(val text: String)
