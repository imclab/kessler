package ksp

import java.io.FileWriter

/* Superclass for types that wrap an in-game SFS object */
class WrappedObject(self: Object) {
  def asObject = self

  override def hashCode = self.hashCode
  override def equals(x: Any) = x match {
    case x: WrappedObject => this.equals(x)
    case x => super.equals(x)
  }
  def equals(x: WrappedObject) = {
    x.asObject == self
  }
  
  def parseProperty(prop: String): (Seq[WrappedObject], String, Int) = {
    def hasPrefix(prop: String) = """^[^:,]+(,(\d+|\*))?:""".r.findFirstMatchIn(prop).isDefined
    def splitPrefix(prop: String) = {
      val groups = """^([^:,]+)(?:,(\d+|\*))?:(.*)""".r.findFirstMatchIn(prop).get.subgroups

      (groups(0),groups(1),groups(2))
    }
    def parsePrefix(objs: Seq[WrappedObject], key: String): (Seq[WrappedObject], String) = {
      val (kind,index,tail): (String,String,String) = splitPrefix(key)

      (index match {
        case "*"        => objs.flatMap(_.asObject.getChildren(kind)).map(new WrappedObject(_))
        case null       => objs.map(_.asObject.getChild(kind)).map(new WrappedObject(_))
        case n: String  => objs.map(_.asObject.getChild(kind, n.toInt)).map(new WrappedObject(_))
      },tail)
    }
    def hasSuffix(key: String) = """.+,\d+$""".r.findFirstMatchIn(key).isDefined
    def parseSuffix(key: String) = {
      val groups = """(.+),(\d+)$""".r.findFirstMatchIn(key).get.subgroups

      (groups(0),groups(1).toInt)
    }

    var (objs, key) = (Seq(this), prop)
    var index = 0

    while (hasPrefix(key))
      parsePrefix(objs, key) match { case (o,k) => objs = o; key = k; }
    
    if (hasSuffix(key))
      parseSuffix(key) match { case (k,i) => key = k; index = i; }

    (objs, key, index)
  }
  
  def setParsedProperties(prop: String, value: String) {
    val (objs, key, index) = parseProperty(prop)
    
    objs foreach {
      _.asObject.setProperty(key, index, value)
    }
  }
  
  def getParsedProperties(prop: String): Seq[String] = {
    val (objs, key, index) = parseProperty(prop)
    
    objs map (_.asObject.getProperty(key, index))
  }
}

/*
 * KSP savegame class.
 *
 * A savegame consists of:
 * - a multimap of properties (we'll probably just store this as a list)
 * - a list of crew
 * - a list of vessels
 *
 * In addition, for forwards compatibility, we need to store a list of all blocks of unknown type so that if,
 * say, STATION blocks get added in .15, the library passes them through safely.
 *
 * For writing out the save, we dump properties, then crew, then vessels, then unknown blocks.
 */
object Game {
  def fromFile(file: String) = SFSParser.parseString(io.Source.fromFile(file) .mkString)
  def fromString(string: String) = SFSParser.parseString(string)
}

class Game(self: Object) extends WrappedObject(self) {
  def mkString = "// KSP Flight State\n// Edited by libKSP\n\n" + self.mkString

  def save(filename: String) {
    val fout = new FileWriter(filename)
    fout.write(mkString)
    fout.close()
  }

  def vessels = self.getChildren("VESSEL") map { new Vessel(_) }

  def clean(p: (Vessel => Boolean)) {
    vessels.filter(p).foreach {
      v => println("\tD " + v.asObject.getProperty("name")); self.deleteChild("VESSEL", v.asObject)
    }
  }
}

class Vessel(self: ksp.Object) extends WrappedObject(self) {
  def isDebris = !(self.getChild("PART", self.getProperty("root").toInt) hasProperty "crew")
  def isImport = self.testProperty("name", """\([^)]+\)( Debris)?$""")
  def isLanded = self.testProperty("sit", """(SPLASHED|LANDED)""")

  val orbit = new Orbit(self.getChild("ORBIT"))

  /* Place this vessel in a stable orbit around the given body, optionally with the given SMA
   * Note that SMA is the semi-major axis of the orbit, *not* the altitude - an SMA of
   */
  def placeInOrbit(ref: Int, sma: Int) {

  }
}

object Orbit {
  case class Body(id: Int, name: String, radius: Double) {}

  val bodies = Seq(
    Body(0, "Kerbol", 1.0),
    Body(1, "Kerbin", 600.0),
    Body(2, "Muna",   200.0)
  )

  def getBody(id: Int) = bodies(id)
  def getBody(name: String) = bodies find (b => b.name equals name) match {
    case Some(b) => b
    case _ => throw new NoSuchElementException(name)
  }
}

class Orbit(self: ksp.Object) extends WrappedObject(self) {
  def orbit(body: Orbit.Body, alt: Double = 100.0, inc: Double = 0.0, ecc: Double = 0.0) {
    /*
    self.setProperty("SMA", (alt + body.radius).toString)
    self.setProperty("ECC", ecc.toString)
    self.setProperty("INC", inc.toString)
    */
  }
}