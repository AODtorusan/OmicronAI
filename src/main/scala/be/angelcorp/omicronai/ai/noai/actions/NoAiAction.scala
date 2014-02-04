package be.angelcorp.omicronai.ai.noai.actions

import scala.collection.JavaConverters._
import org.newdawn.slick.{Color, Graphics}
import com.lyndir.omicron.api.model.{PublicModuleType, UnitTypes, UnitType, WeaponModule}
import com.lyndir.omicron.api.model.IConstructorModule.{OutOfRangeException, IncompatibleLevelException, InaccessibleException}
import be.angelcorp.omicronai.{HexTile, Location}
import be.angelcorp.omicronai.assets.Asset
import be.angelcorp.omicronai.ai.noai.NoAi
import be.angelcorp.omicronai.algorithms.AStar
import be.angelcorp.omicronai.gui.layerRender.{PolyLineRenderer, MultiplexRenderer, LayerRenderer}
import be.angelcorp.omicronai.gui.{Canvas, ViewPort}
import be.angelcorp.omicronai.gui.textures.MapIcons
import com.typesafe.scalalogging.slf4j.Logger
import org.slf4j.LoggerFactory
import be.angelcorp.omicronai.gui.slick.DrawStyle

trait NoAiAction {

  def execute(noai: NoAi): Option[NoAiAction]

  def preview: LayerRenderer

}

case class SequencedAction( actions: Seq[NoAiAction] ) extends NoAiAction {
  lazy val preview: LayerRenderer = new MultiplexRenderer( actions.map( _.preview ) )

  override def execute(noai: NoAi): Option[NoAiAction] = {
    val nextActions = actions.foldLeft(Nil: List[NoAiAction])( (nextActions, action) =>
      if (nextActions.isEmpty)
        action.execute(noai).toList
      else
        nextActions :+ action
    )
    if (nextActions.isEmpty) None else Some(SequencedAction(nextActions))
  }
}

case class MoveAction( asset: Asset, destination: Location ) extends NoAiAction {
  lazy val solution = AStar(destination).findPath(asset.location)
  lazy val preview  = solution._2.layers.get("Solution path").get
  def execute(noai: NoAi) = {
    noai.move(asset, solution._1.path.reverse)
    if (asset.location != destination)
      Some( new MoveAction(asset, destination) )
    else None
  }
}

case class MoveInRangeAction( asset: Asset, destination: Location, range: Int ) extends NoAiAction {
  lazy val path = {
    val completePath = AStar(destination).findPath(asset.location)._1.path.reverse
    completePath.takeWhile( step => (step δ destination) > (range - 1)  ) // -1 because to step into the required range
  }
  lazy val preview = new PolyLineRenderer(path, Color.yellow)
  def execute(noai: NoAi) = {
    noai.move(asset, path)
    if (asset.location != destination)
      Some( new MoveAction(asset, destination) )
    else None
  }
}

case class FireAction( asset: Asset, module: WeaponModule, destination: Location ) extends NoAiAction {
  val preview = new LayerRenderer {
    val (toX, toY) = (destination: HexTile).centerXY
    def render(g: Graphics, view: ViewPort) {
      val (fromX, fromY) = (asset.location: HexTile).centerXY
      g.setColor( Color.red )
      g.drawLine(fromX * Canvas.scale, fromY * Canvas.scale, toX * Canvas.scale, toY * Canvas.scale)
    }
  }
  def execute(noai: NoAi) = {
    if (noai.attack(asset, module, destination)) None
    else Some(this)
  }
}

case class ConstructionStartAction( builder: Asset, destination: Location, constructedType: UnitType ) extends NoAiAction {
  val logger = Logger( LoggerFactory.getLogger( getClass ) )
  lazy val preview = new LayerRenderer {
    var t = System.currentTimeMillis()
    var isFlashing = true
    override def render(g: Graphics, view: ViewPort) {
      if (System.currentTimeMillis() - t > 500) {
        t = System.currentTimeMillis()
        isFlashing = !isFlashing
      }
      val img  = MapIcons.getIcon( UnitTypes.CONSTRUCTION )
      val tile = HexTile(destination)
      val center = Canvas.center(tile)
      if (isFlashing)
        img.drawFlash( center._1 - img.getWidth/2, center._2 - img.getHeight/2 )
      else
        img.draw( center._1 - img.getWidth/2, center._2 - img.getHeight/2 )
      Canvas.line( g, builder.location, tile, DrawStyle(Color.blue, 2) )
    }
  }

  override def execute(noai: NoAi): Option[NoAiAction] = {
    val constructionModules = builder.gameObject.getModules( PublicModuleType.CONSTRUCTOR ).asScala
    if (constructionModules.isEmpty)
      Some(this) // TODO: handle missing constructor module
    else {
      implicit val game = noai.game
      try {
        val site = constructionModules.head.schedule( constructedType, destination )
        constructionModules.foreach( _.setTarget(site) )
      } catch {
        case e: InaccessibleException =>
          logger.info(s"$builder could not build $constructedType at that $destination, tile not accessible!", e)
          Some(this)
        case e: IncompatibleLevelException =>
          logger.info(s"$builder could not build $constructedType at that $destination, incorrect level!", e)
          Some(this)
        case e: OutOfRangeException =>
          new SequencedAction( Seq( MoveInRangeAction(builder, destination, 1), this ) )
      }
      None
    }
  }

}

case class ConstructionAssistAction( builder: Asset, destination: Location) extends NoAiAction {
  lazy val preview = new PolyLineRenderer( Seq(builder.location, destination), DrawStyle(Color.blue, 2) )
  override def execute(noai: NoAi): Option[NoAiAction] = {
    val constructionModules = builder.gameObject.getModules( PublicModuleType.CONSTRUCTOR ).asScala
    if (constructionModules.isEmpty)
      Some(this) // TODO: handle missing constructor module
    else {
      noai.unitOn(destination) match {
        case Some(site) => constructionModules.foreach( _.setTarget( site.gameObject ) )
        case _ => ???
      }
      None
    }
  }
}
