package org.endera.enderalib.utils.async

import kotlinx.coroutines.Dispatchers

val ioDispatcher = Dispatchers.IO.limitedParallelism(256)