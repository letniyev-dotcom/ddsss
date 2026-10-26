package team.letniev.tboard

/** иконки экрана настроек (пути из HTML, сетка 24x24) */
internal object AppIcons {
    private const val DOTS = 2.6f
    val check = Ic(listOf(P("m5 12.5 4.5 4.5L19 7.5")))
    val spell = Ic(listOf(P("M3.5 17.5 8 6l4.5 11.5M5.2 13h5.6"), P("m14.5 14 2.5 2.5 4.5-5")))
    val words = Ic(listOf(P(rr(3f, 6.5f, 18f, 11f, 3.5f)), P("M7.5 12h.01M12 12h.01M16.5 12h.01", DOTS)))
    val vib = Ic(listOf(P(rr(8f, 3.5f, 8f, 17f, 2.5f)), P("M4.5 8v8M19.5 8v8")))
    val trash = Ic(listOf(P("M5 7h14M10 7V4.5h4V7M7 7l1 13h8l1-13M10.5 11v5M13.5 11v5")))
    val sun = Ic(
        listOf(
            P("M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0z"),
            P("M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4"),
        ),
    )
    val moon = Ic(listOf(P("M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z")))
    val tog = Ic(listOf(P(rr(3f, 7f, 18f, 10f, 5f)), P("M18.6 12a2.6 2.6 0 1 1-5.2 0 2.6 2.6 0 0 1 5.2 0z")))
    val caps = Ic(listOf(P("M4 18 9.5 5 15 18M6 14h7M18 9v9m0 0 2-2m-2 2-2-2")))
    val hint = Ic(listOf(P(rr(4f, 5f, 16f, 14f, 3.5f)), P("M15.5 9h.01M8.5 15h5", 2.2f)))
}
