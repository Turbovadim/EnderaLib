package org.endera.enderalib.utils.configuration

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Comment(val text: String)
