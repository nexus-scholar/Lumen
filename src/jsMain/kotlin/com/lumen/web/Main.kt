package com.lumen.web

import androidx.compose.runtime.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable

fun main() {
    renderComposable(rootElementId = "root") {
        Style(AppStylesheet)
        App()
    }
}

@Composable
fun App() {
    Div({ classes(AppStylesheet.container) }) {
        H1({ classes(AppStylesheet.title) }) {
            Text("Lumen - Systematic Review Tool")
        }

        P({ classes(AppStylesheet.subtitle) }) {
            Text("AI-Powered Systematic Review Assistant")
        }

        Div({ classes(AppStylesheet.buttonContainer) }) {
            Button({
                classes(AppStylesheet.primaryButton)
                onClick { /* TODO: Create new project */ }
            }) {
                Text("Create New Project")
            }

            Button({
                classes(AppStylesheet.secondaryButton)
                onClick { /* TODO: Open existing project */ }
            }) {
                Text("Open Existing Project")
            }
        }
    }
}

object AppStylesheet : StyleSheet() {
    val container by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        alignItems(AlignItems.Center)
        justifyContent(JustifyContent.Center)
        height(100.vh)
        padding(16.px)
    }

    val title by style {
        fontSize(2.5.em)
        fontWeight("bold")
        marginBottom(16.px)
    }

    val subtitle by style {
        fontSize(1.2.em)
        marginBottom(32.px)
        color(Color("#666"))
    }

    val buttonContainer by style {
        display(DisplayStyle.Flex)
        flexDirection(FlexDirection.Column)
        gap(16.px)
    }

    val primaryButton by style {
        padding(12.px, 24.px)
        fontSize(1.em)
        backgroundColor(Color("#1976d2"))
        color(Color.white)
        border(0.px)
        borderRadius(4.px)
        cursor("pointer")
        property("transition", "background-color 0.3s")

        hover(self) style {
            backgroundColor(Color("#1565c0"))
        }
    }

    val secondaryButton by style {
        padding(12.px, 24.px)
        fontSize(1.em)
        backgroundColor(Color.white)
        color(Color("#1976d2"))
        border(1.px, LineStyle.Solid, Color("#1976d2"))
        borderRadius(4.px)
        cursor("pointer")
        property("transition", "all 0.3s")

        hover(self) style {
            backgroundColor(Color("#e3f2fd"))
        }
    }
}

