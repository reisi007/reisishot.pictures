package pictures.reisishot.mise.backend.main

object ProductionAll {

    @JvmStatic
    fun main(args: Array<String>) {
        System.out.println(">> Main")
        Main.build(false)
        System.out.println(">> Couples")
        Couples.build(false)
        System.out.println(">> Portrait")
        Portrait.build(false)
        System.out.println(">> Boudoir")
        Boudoir.build(false)
        System.out.println(">> Goto")
        Goto.build(false)
    }
}
