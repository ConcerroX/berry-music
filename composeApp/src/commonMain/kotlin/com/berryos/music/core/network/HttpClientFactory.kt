package com.berryos.music.core.network

import io.ktor.client.HttpClient

expect fun createJsonHttpClient(): HttpClient
