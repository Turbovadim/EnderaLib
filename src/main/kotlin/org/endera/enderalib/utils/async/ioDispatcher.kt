package org.endera.enderalib.utils.async

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
val ioDispatcher = Dispatchers.IO.limitedParallelism(256)