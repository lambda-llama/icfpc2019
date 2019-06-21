import tornadofx.*

class HelloWorld : View() {
    override val root = hbox {
        label("Hello world")
    }
}
class HelloWorldApp : App(HelloWorld::class, Styles::class)

class Styles : Stylesheet() {
    init {
        label {
            fontSize = 20.px
        }
    }
}


fun main(args: Array<String>) {
    launch<HelloWorldApp>()
}
