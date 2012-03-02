package ksp

import java.io.File

object GameEditor {
  def main(args:Array[String]) {
    val filename = if(args.length > 0) {
      args(0)
    } else {
      ask("Enter location of save file:")
    }
    val game = Game.fromFile(filename)

    askAndThen("Clean imported debris?") {
      () => game.clean(v => v.isDebris && v.isImport)
    }
    askAndThen("Clean landed/splashed debris?") {
      () => game.clean(v => v.isDebris && v.isLanded)
    }
    askAndThen("Clean all other debris?") {
      () => game.clean(v => v.isDebris)
    }
    askAndThen("Clean all imported ships?") {
      () => game.clean(v => !(v.isDebris) && v.isImport)
    }
    askAndThen("Clean landed/splashed ships?") {
      () => game.clean(v => !(v.isDebris) && v.isLanded)
    }
    askAndThen("Clean all other ships?") {
      () => game.clean(v => v.isDebris)
    }

    println("Backing up original savegames...")
    new File(filename).renameTo(new File(filename + "." + timestamp))

    println("Writing new savegame...")
    game.save(filename)
  }

  def timestamp:String = new java.text.SimpleDateFormat("yyyy-MM-dd@hh-mm-ss").format(new java.util.Date())

  def ask(question:String):String = {
    print(question + " ")
    readLine()
  }

  def askYN(question:String):Boolean = {
    val answer = ask(question + " [y/N]:")
    answer.startsWith("y") || answer.startsWith("Y")
  }

  def askAndThen(question:String) = {
    (p: () => Unit) => {
      if(askYN(question)) p()
    }
  }
}

