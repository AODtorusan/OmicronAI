package be.angelcorp.omicronai.gui.slick

import scala.math._
import scala.collection.mutable
import java.awt.{Dimension, Rectangle}

class MaxRectsBinPack(var binWidth: Int = 0, var binHeight: Int = 0) {

  init(binWidth, binHeight)

  private val usedRectangles = mutable.ListBuffer[Rectangle]()
  private val freeRectangles = mutable.ListBuffer[Rectangle]()

  /**
   * (Re)initializes the packer to an empty bin of width x height units. Call whenever you need to restart with a new bin.
   */
  def init(width: Int, height: Int) {
    binWidth = width
    binHeight = height

    val n = new Rectangle(0, 0, width, height)

    usedRectangles.clear()

    freeRectangles.clear()
    freeRectangles += n
  }

  /// Inserts a single rectangle into the bin, possibly rotated.
  def insert(width: Int, height: Int, method: FreeRectChoiceHeuristic): Rectangle = {
    val (newNode, _, _) = method match {
      case RectBestShortSideFit() => findPositionForNewNodeBestShortSideFit(width, height)
      case RectBottomLeftRule()   => findPositionForNewNodeBottomLeft(width, height)
      case RectContactPointRule() =>
        val (newNode, score1) = findPositionForNewNodeContactPoint(width, height)
        (newNode, score1, Int.MaxValue)
      case RectBestLongSideFit()  => findPositionForNewNodeBestLongSideFit(width, height)
      case RectBestAreaFit()      => findPositionForNewNodeBestAreaFit(width, height)
    }

    if (newNode.height == 0)
      return newNode

    var i = 0
    var numRectanglesToProcess = freeRectangles.size
    while(i < numRectanglesToProcess) {
      if (splitFreeNode(freeRectangles(i), newNode)) {
        freeRectangles remove i
        i -= 1
        numRectanglesToProcess -= 1
      }
      i += 1
    }

    pruneFreeList()

    usedRectangles += newNode
    newNode
  }

  /// Inserts the given list of rectangles in an offline/batch mode, possibly rotated.
  /// @param rects The list of rectangles to insert. This vector will be destroyed in the process.
  /// @param dst [out] This list will contain the packed rectangles. The indices will not correspond to that of rects.
  /// @param method The rectangle placement rule to use when packing.
  def insert(rects: mutable.ListBuffer[Dimension], dst: mutable.ListBuffer[Rectangle], method: FreeRectChoiceHeuristic) {
    dst.clear()

    while(rects.size > 0) {
      var bestScore1 = Int.MaxValue
      var bestScore2 = Int.MaxValue
      var bestRectIndex = -1
      var bestNode: Rectangle = null

      for(i <- 0 until rects.size) {
        val rect = rects(i)
        val (newNode, score1, score2) = scoreRect(rect.width, rect.height, method)

        if (score1 < bestScore1 || (score1 == bestScore1 && score2 < bestScore2)) {
          bestScore1 = score1
          bestScore2 = score2
          bestNode = newNode
          bestRectIndex = i
        }
      }

      if (bestRectIndex != -1) {
        placeRect(bestNode)
        rects.remove(bestRectIndex)
      }
    }
  }

  /// Computes the ratio of used surface area to the total bin area.
  def occupancy = {
    val surface = usedRectangles.foldLeft(0.0)( (surface, rect) => surface + rect.getWidth * rect.getHeight )
    surface / (binWidth * binHeight)
  }

  /// Computes the placement score for placing the given rectangle with the given method.
  /// @param score1 [out] The primary placement score will be outputted here.
  /// @param score2 [out] The secondary placement score will be outputted here. This isu sed to break ties.
  /// @return This struct identifies where the rectangle would be placed if it were placed.
  private def scoreRect(width: Int, height: Int, method: FreeRectChoiceHeuristic): (Rectangle, Int, Int) = {
    val (newNode, score1, score2) = method match {
      case RectBestShortSideFit() =>
        findPositionForNewNodeBestShortSideFit(width, height)
      case RectBottomLeftRule() =>
        findPositionForNewNodeBottomLeft(width, height)
      case RectContactPointRule() =>
        val (newNode_, score1_) = findPositionForNewNodeContactPoint(width, height)
        (newNode_, -score1_, Int.MaxValue) // Reverse since we are minimizing, but for contact point score bigger is better.
      case RectBestLongSideFit() =>
        findPositionForNewNodeBestLongSideFit(width, height)
      case RectBestAreaFit() =>
        findPositionForNewNodeBestAreaFit(width, height)
    }

    // Cannot fit the current rectangle.
    if (newNode.height == 0)
      (newNode, Int.MaxValue, Int.MaxValue)
    else
      (newNode, score1, score2)
  }

  /// Places the given rectangle into the bin.
  private def placeRect(node: Rectangle) {
    var numRectanglesToProcess = freeRectangles.size
    var i = 0
    while(i < numRectanglesToProcess) {
      if (splitFreeNode(freeRectangles(i), node)) {
        freeRectangles.remove( i )
        i -= 1
        numRectanglesToProcess -= 1
      }
      i += 1
    }

    pruneFreeList()

    usedRectangles += node
    // dst.push_back(bestNode); ///\todo Refactor so that this compiles.
  }

  /// Computes the placement score for the -CP variant.
  private def contactPointScoreNode(x: Int, y: Int, width: Int, height: Int): Int = {
    var score = 0

    if (x == 0 || x + width == binWidth)
      score += height
    if (y == 0 || y + height == binHeight)
      score += width

    for(rect <- usedRectangles) {
      if (rect.x == x + width || rect.x + rect.width == x)
        score += commonIntervalLength(rect.y, rect.y + rect.height, y, y + height)
      if (rect.y == y + height || rect.y + rect.height == y)
        score += commonIntervalLength(rect.x, rect.x + rect.width, x, x + width)
    }
    score
  }

  private def findPositionForNewNodeBottomLeft(width: Int, height: Int): (Rectangle, Int, Int) = {
    var bestNode: Rectangle = null
    var bestX = 0
    var bestY = Int.MaxValue

    for(rect <- freeRectangles) {
      // Try to place the rectangle in upright (non-flipped) orientation.
      if (rect.width >= width && rect.height >= height) {
        val topSideY = rect.y + height
        if (topSideY < bestY || (topSideY == bestY && rect.x < bestX)) {
          bestNode = new Rectangle(rect.x, rect.y, width, height)
          bestY = topSideY
          bestX = rect.x
        }
      }
      if (rect.width >= height && rect.height >= width) {
        val topSideY = rect.y + width
        if (topSideY < bestY || (topSideY == bestY && rect.x < bestX)) {
          bestNode = new Rectangle(rect.x, rect.y, width, height)
          bestY = topSideY
          bestX = rect.x
        }
      }
    }
    (bestNode, bestY, bestX)
  }

  private def findPositionForNewNodeBestShortSideFit(width: Int, height: Int): (Rectangle, Int, Int) = {
    var bestNode: Rectangle = null
    var bestShortSideFit = Int.MaxValue
    var bestLongSideFit  = Int.MaxValue

    for(rect <- freeRectangles) {
      // Try to place the rectangle in upright (non-flipped) orientation.
      if (rect.width >= width && rect.height >= height) {
        val leftoverHoriz = abs(rect.width  - width )
        val leftoverVert  = abs(rect.height - height)
        val shortSideFit  = min(leftoverHoriz, leftoverVert)
        val longSideFit   = max(leftoverHoriz, leftoverVert)

        if (shortSideFit < bestShortSideFit || (shortSideFit == bestShortSideFit && longSideFit < bestLongSideFit)) {
          bestNode = new Rectangle(rect.x, rect.y, width, height)
          bestShortSideFit = shortSideFit
          bestLongSideFit = longSideFit
        }
      }

      if (rect.width >= height && rect.height >= width) {
        val flippedLeftoverHoriz = abs(rect.width - height)
        val flippedLeftoverVert  = abs(rect.height - width)
        val flippedShortSideFit  = min(flippedLeftoverHoriz, flippedLeftoverVert)
        val flippedLongSideFit   = max(flippedLeftoverHoriz, flippedLeftoverVert)

        if (flippedShortSideFit < bestShortSideFit || (flippedShortSideFit == bestShortSideFit && flippedLongSideFit < bestLongSideFit)) {
          bestNode = new Rectangle(rect.x, rect.y, width, height)
          bestShortSideFit = flippedShortSideFit
          bestLongSideFit  = flippedLongSideFit
        }
      }
    }
    (bestNode, bestShortSideFit, bestLongSideFit)
  }

  private def findPositionForNewNodeBestLongSideFit(width: Int, height: Int): (Rectangle, Int, Int) = {
    var bestNode: Rectangle = null
    var bestLongSideFit  = Int.MaxValue
    var bestShortSideFit = Int.MaxValue

    for(rect <- freeRectangles) {
      // Try to place the rectangle in upright (non-flipped) orientation.
      if (rect.width >= width && rect.height >= height) {
        val leftoverHoriz = abs(rect.width  - width )
        val leftoverVert  = abs(rect.height - height)
        val shortSideFit  = min(leftoverHoriz, leftoverVert)
        val longSideFit   = max(leftoverHoriz, leftoverVert)

        if (longSideFit < bestLongSideFit || (longSideFit == bestLongSideFit && shortSideFit < bestShortSideFit)) {
          bestNode = new Rectangle(rect.x, rect.y, width, height)
          bestShortSideFit = shortSideFit
          bestLongSideFit = longSideFit
        }
      }

      if (rect.width >= height && rect.height >= width) {
        val leftoverHoriz = abs(rect.width - height)
        val leftoverVert  = abs(rect.height - width)
        val shortSideFit  = min(leftoverHoriz, leftoverVert)
        val longSideFit   = max(leftoverHoriz, leftoverVert)

        if (longSideFit < bestLongSideFit || (longSideFit == bestLongSideFit && shortSideFit < bestShortSideFit)) {
          bestNode = new Rectangle(rect.x, rect.y, width, height)
          bestShortSideFit = shortSideFit
          bestLongSideFit = longSideFit
        }
      }
    }
    (bestNode, bestShortSideFit, bestLongSideFit)
  }

  private def findPositionForNewNodeBestAreaFit(width: Int, height: Int): (Rectangle, Int, Int) = {
    var bestNode: Rectangle = null
    var bestAreaFit = Int.MaxValue
    var bestShortSideFit = Int.MaxValue

    for(rect <- freeRectangles) {
      val areaFit = rect.width * rect.height - width * height

      // Try to place the rectangle in upright (non-flipped) orientation.
      if (rect.width >= width && rect.height >= height) {
        val leftoverHoriz = abs(rect.width  - width )
        val leftoverVert  = abs(rect.height - height)
        val shortSideFit  = min(leftoverHoriz, leftoverVert)

        if (areaFit < bestAreaFit || (areaFit == bestAreaFit && shortSideFit < bestShortSideFit)) {
          bestNode = new Rectangle(rect.x, rect.y, width, height)
          bestShortSideFit = shortSideFit
          bestAreaFit = areaFit
        }
      }

      if (rect.width >= height && rect.height >= width) {
        val leftoverHoriz = abs(rect.width - height)
        val leftoverVert  = abs(rect.height - width)
        val shortSideFit  = min(leftoverHoriz, leftoverVert)

        if (areaFit < bestAreaFit || (areaFit == bestAreaFit && shortSideFit < bestShortSideFit)) {
          bestNode = new Rectangle(rect.x, rect.y, width, height)
          bestShortSideFit = shortSideFit
          bestAreaFit = areaFit
        }
      }
    }
    (bestNode, bestAreaFit, bestShortSideFit)
  }

  private def findPositionForNewNodeContactPoint(width: Int, height: Int): (Rectangle, Int) = {
    var bestNode: Rectangle = null
    var bestContactScore = -1

    for(rect <- freeRectangles) {
      // Try to place the rectangle in upright (non-flipped) orientation.
      if (rect.width >= width && rect.height >= height) {
        val score = contactPointScoreNode(rect.x, rect.y, width, height)
        if (score > bestContactScore) {
          bestNode = new Rectangle(rect.x, rect.y, width, height)
          bestContactScore = score
        }
      }
      if (rect.width >= height && rect.height >= width) {
        val score = contactPointScoreNode(rect.x, rect.y, height, width)
        if (score > bestContactScore) {
          bestNode = new Rectangle(rect.x, rect.y, width, height)
          bestContactScore = score
        }
      }
    }
    (bestNode, bestContactScore)
  }

  /// @return True if the free node was split.
  private def splitFreeNode(freeNode: Rectangle, usedNode: Rectangle): Boolean = {
    // Test with SAT if the rectangles even intersect.
    if (usedNode.x >= freeNode.x + freeNode.width  || usedNode.x + usedNode.width  <= freeNode.x || usedNode.y >= freeNode.y + freeNode.height || usedNode.y + usedNode.height <= freeNode.y) {
      false
    } else {
      if (usedNode.x < freeNode.x + freeNode.width && usedNode.x + usedNode.width > freeNode.x) {
        // New node at the top side of the used node.
        if (usedNode.y > freeNode.y && usedNode.y < freeNode.y + freeNode.height) {
          val newNode = new Rectangle( freeNode )
          newNode.height = usedNode.y - newNode.y
          freeRectangles += newNode
        }

        // New node at the bottom side of the used node.
        if (usedNode.y + usedNode.height < freeNode.y + freeNode.height) {
          val newNode = new Rectangle( freeNode )
          newNode.y = usedNode.y + usedNode.height
          newNode.height = freeNode.y + freeNode.height - (usedNode.y + usedNode.height)
          freeRectangles += newNode
        }
      }

      if (usedNode.y < freeNode.y + freeNode.height && usedNode.y + usedNode.height > freeNode.y) {
        // New node at the left side of the used node.
        if (usedNode.x > freeNode.x && usedNode.x < freeNode.x + freeNode.width) {
          val newNode = new Rectangle( freeNode )
          newNode.width = usedNode.x - newNode.x
          freeRectangles += newNode
        }

        // New node at the right side of the used node.
        if (usedNode.x + usedNode.width < freeNode.x + freeNode.width) {
          val newNode = new Rectangle( freeNode )
          newNode.x = usedNode.x + usedNode.width
          newNode.width = freeNode.x + freeNode.width - (usedNode.x + usedNode.width)
          freeRectangles += newNode
        }
      }

      true
    }
  }

  /// Goes through the free rectangle list and removes any redundant entries.
  private def pruneFreeList() {
    /// Go through each pair and remove any rectangle that is redundant.
    var i = 0
    while(i < freeRectangles.size) {
      var j = i+1
      var doBreak = false
      while(j < freeRectangles.size && !doBreak) {
        if (isContainedIn(freeRectangles(i), freeRectangles(j))) {
          freeRectangles.remove(i)
          i -= 1
          doBreak = true
        }
        if (isContainedIn(freeRectangles(j), freeRectangles(i)) && !doBreak ) {
          freeRectangles.remove(j)
          j -= 0
        }
        j += 1
      }
      i += 1
    }
  }

  /// Returns 0 if the two intervals i1 and i2 are disjoint, or the length of their overlap otherwise.
  private def commonIntervalLength(i1start: Int, i1end: Int, i2start: Int, i2end: Int): Int =
    if (i1end < i2start || i2end < i1start) 0 else min(i1end, i2end) - max(i1start, i2start)

  private def isContainedIn(a: Rectangle, b: Rectangle): Boolean =
    a.x >= b.x && a.y >= b.y && a.x+a.width <= b.x+b.width && a.y+a.height <= b.y+b.height

}

/** Specifies the different heuristic rules that can be used when deciding where to place a new rectangle. */
sealed abstract class FreeRectChoiceHeuristic
/** BSSF: Positions the rectangle against the short side of a free rectangle into which it fits the best. */
case class RectBestShortSideFit() extends FreeRectChoiceHeuristic
/** BLSF: Positions the rectangle against the long side of a free rectangle into which it fits the best. */
case class RectBestLongSideFit() extends FreeRectChoiceHeuristic
/** BAF: Positions the rectangle into the smallest free rect into which it fits. */
case class RectBestAreaFit() extends FreeRectChoiceHeuristic
/** BL: Does the Tetris placement. */
case class RectBottomLeftRule() extends FreeRectChoiceHeuristic
/** CP: Choosest the placement where the rectangle touches other rects as much as possible. */
case class RectContactPointRule() extends FreeRectChoiceHeuristic
