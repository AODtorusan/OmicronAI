package be.angelcorp.omicron.base.sprites.spriteSheet

import scala.collection.mutable.ListBuffer
import be.angelcorp.omicron.base.sprites.BufferedSprite

/**
 * Algorithm based on code at: https://github.com/juj/RectangleBinPack
 */
class MaxRectsBinPack(val binWidth: Int = 0, val binHeight: Int = 0) {
  import MaxRectsBinPack._
  
  private val usedRectangles = ListBuffer[Rect]()
  private val freeRectangles = ListBuffer[Rect]()
  freeRectangles += Rect(0, 0, binWidth, binHeight)

  /// Inserts the given list of rectangles in an offline/batch mode, possibly rotated.
  /// @param rects The list of rectangles to insert. This vector will be destroyed in the process.
  /// @param dst [out] This list will contain the packed rectangles. The indices will not correspond to that of rects.
  /// @param method The rectangle placement rule to use when packing.
  def insert(rects: Seq[BufferedSprite], method: FreeRectChoiceHeuristic) = {
    val toPlace = ListBuffer( rects: _* )
    val places  = Map.newBuilder[BufferedSprite, Rect]
    while (toPlace.size > 0) {
      var bestScore1 = Int.MaxValue
      var bestScore2 = Int.MaxValue
      var bestRectIndex = -1
      var bestNode: Rect = null

      for (i <- 0 until toPlace.size) {
        val (newNode, score1, score2) = scoreRect(toPlace(i).bufferedSprite.getWidth, toPlace(i).bufferedSprite.getHeight, method)
        if (score1 < bestScore1 || (score1 == bestScore1 && score2 < bestScore2)) {
          bestScore1 = score1
          bestScore2 = score2
          bestNode = newNode
          bestRectIndex = i
        }
      }

      if (bestRectIndex == -1)
        throw new OutOfSpace()

      placeRect(bestNode)
      places += (toPlace(bestRectIndex) -> bestNode)
      toPlace.remove(bestRectIndex)
    }
    places.result()
  }

  /** Inserts a single rectangle into the bin, possibly rotated. */
  def insert(width: Int, height: Int, method: FreeRectChoiceHeuristic): Rect = {
    val newNode = method match {
      case RectBestShortSideFit => FindPositionForNewNodeBestShortSideFit(width, height)._1
      case RectBottomLeftRule   => FindPositionForNewNodeBottomLeft(width, height)._1
      case RectContactPointRule => FindPositionForNewNodeContactPoint(width, height)._1
      case RectBestLongSideFit  => FindPositionForNewNodeBestLongSideFit(width, height)._1
      case RectBestAreaFit      => FindPositionForNewNodeBestAreaFit(width, height)._1
    }

    if (newNode.height == 0)
      throw new OutOfSpace()

    freeRectangles --= freeRectangles.filter( rect => splitFreeNode(rect, newNode) )
    pruneFreeList()

    usedRectangles += newNode
    newNode
  }

  /// Computes the ratio of used surface area to the total bin area.
  def occupancy() =
    usedRectangles.foldLeft(0.0)( (sum, r) => sum + r.width * r.height ) / (binWidth * binHeight)


  /// Computes the placement score for placing the given rectangle with the given method.
  /// @param score1 [out] The primary placement score will be outputted here.
  /// @param score2 [out] The secondary placement score will be outputted here. This isu sed to break ties.
  /// @return This struct identifies where the rectangle would be placed if it were placed.
  private def scoreRect(width: Int, height: Int, method: FreeRectChoiceHeuristic): (Rect, Int, Int) = {
    var (newNode, score1, score2) = method match {
      case RectBestShortSideFit =>
        val (_newNode, _score1, _score2) = FindPositionForNewNodeBestShortSideFit(width, height)
        (_newNode, _score1, _score2)
      case RectBottomLeftRule =>
        val (_newNode, _score1, _score2) = FindPositionForNewNodeBottomLeft(width, height)
        (_newNode, _score1, _score2)
      case RectContactPointRule =>
        val (_newNode, _score1) = FindPositionForNewNodeContactPoint(width, height)
        (_newNode, - _score1, Int.MaxValue) // Reverse since we are minimizing, but for contact point score bigger is better.
      case RectBestLongSideFit =>
        val (_newNode, _score2, _score1) = FindPositionForNewNodeBestLongSideFit(width, height)
        (_newNode, _score1, _score2)

      case RectBestAreaFit =>
        val (_newNode, _score1, _score2) = FindPositionForNewNodeBestAreaFit(width, height)
        (_newNode, _score1, _score2)
    }

    // Cannot fit the current rectangle.
    if (newNode.height == 0) {
      score1 = Int.MaxValue
      score2 = Int.MaxValue
    }

    (newNode, score1, score2)
  }

  /// Places the given rectangle into the bin.
  private def placeRect(node: Rect) {
    freeRectangles --= freeRectangles.filter( rect => splitFreeNode(rect, node) )
    pruneFreeList()
    usedRectangles += node
  }

  /// Computes the placement score for the -CP variant.
  private def ContactPointScoreNode(x: Int, y: Int, width: Int, height: Int) = {
    var score = 0

    if (x == 0 || x + width == binWidth)
      score += height
    if (y == 0 || y + height == binHeight)
      score += width

    for (rect <- usedRectangles) {
      if (rect.x == x + width || rect.x + rect.width == x)
        score += CommonIntervalLength(rect.y, rect.y + rect.height, y, y + height)
      if (rect.y == y + height || rect.y + rect.height == y)
        score += CommonIntervalLength(rect.x, rect.x + rect.width, x, x + width)
    }
    score
  }

  private def FindPositionForNewNodeBottomLeft(width: Int, height: Int) = {
    var bestNode = Rect(0, 0, 0, 0)
    var bestX = Int.MaxValue
    var bestY = Int.MaxValue

    for (rect <- freeRectangles) {
      // Try to place the rectangle in upright (non-flipped) orientation.
      if (rect.width >= width && rect.height >= height) {
        val topSideY = rect.y + height
        if (topSideY < bestY || (topSideY == bestY && rect.x < bestX)) {
          bestNode = Rect(rect.x, rect.y, width, height)
          bestY = topSideY
          bestX = rect.x
        }
      }
      if (rect.width >= height && rect.height >= width) {
        val topSideY = rect.y + width
        if (topSideY < bestY || (topSideY == bestY && rect.x < bestX)) {
          bestNode = Rect(rect.x, rect.y, width, height)
          bestY = topSideY
          bestX = rect.x
        }
      }
    }
    (bestNode, bestY, bestX)
  }

  private def FindPositionForNewNodeBestShortSideFit(width: Int, height: Int) = {
    var bestNode = Rect(0, 0, 0, 0)
    var bestShortSideFit = Int.MaxValue
    var bestLongSideFit = Int.MaxValue

    for (rect <- freeRectangles) {
      // Try to place the rectangle in upright (non-flipped) orientation.
      if (rect.width >= width && rect.height >= height) {
        val leftoverHoriz = math.abs(rect.width - width);
        val leftoverVert = math.abs(rect.height - height);
        val shortSideFit = math.min(leftoverHoriz, leftoverVert);
        val longSideFit = math.max(leftoverHoriz, leftoverVert);

        if (shortSideFit < bestShortSideFit || (shortSideFit == bestShortSideFit && longSideFit < bestLongSideFit)) {
          bestNode = Rect(rect.x, rect.y, width, height)
          bestShortSideFit = shortSideFit;
          bestLongSideFit = longSideFit;
        }
      }

      if (rect.width >= height && rect.height >= width) {
        val flippedLeftoverHoriz = math.abs(rect.width - height);
        val flippedLeftoverVert = math.abs(rect.height - width);
        val flippedShortSideFit = math.min(flippedLeftoverHoriz, flippedLeftoverVert);
        val flippedLongSideFit = math.max(flippedLeftoverHoriz, flippedLeftoverVert);

        if (flippedShortSideFit < bestShortSideFit || (flippedShortSideFit == bestShortSideFit && flippedLongSideFit < bestLongSideFit)) {
          bestNode = Rect(rect.x, rect.y, width, height)
          bestShortSideFit = flippedShortSideFit;
          bestLongSideFit = flippedLongSideFit;
        }
      }
    }
    (bestNode, bestShortSideFit, bestLongSideFit)
  }

  private def FindPositionForNewNodeBestLongSideFit(width: Int, height: Int) = {
    var bestNode = Rect(0, 0, 0, 0)
    var bestLongSideFit = Int.MaxValue
    var bestShortSideFit = Int.MaxValue

    for (rect <- freeRectangles) {
      // Try to place the rectangle in upright (non-flipped) orientation.
      if (rect.width >= width && rect.height >= height) {
        val leftoverHoriz = math.abs(rect.width - width)
        val leftoverVert = math.abs(rect.height - height)
        val shortSideFit = math.min(leftoverHoriz, leftoverVert)
        val longSideFit = math.max(leftoverHoriz, leftoverVert)

        if (longSideFit < bestLongSideFit || (longSideFit == bestLongSideFit && shortSideFit < bestShortSideFit)) {
          bestNode = Rect(rect.x, rect.y, width, height)
          bestShortSideFit = shortSideFit
          bestLongSideFit = longSideFit
        }
      }

      if (rect.width >= height && rect.height >= width) {
        val leftoverHoriz = math.abs(rect.width - height)
        val leftoverVert = math.abs(rect.height - width)
        val shortSideFit = math.min(leftoverHoriz, leftoverVert)
        val longSideFit = math.max(leftoverHoriz, leftoverVert)

        if (longSideFit < bestLongSideFit || (longSideFit == bestLongSideFit && shortSideFit < bestShortSideFit)) {
          bestNode = Rect(rect.x, rect.y, width, height)
          bestShortSideFit = shortSideFit
          bestLongSideFit = longSideFit
        }
      }
    }
    (bestNode, bestShortSideFit, bestLongSideFit)
  }

  private def FindPositionForNewNodeBestAreaFit(width: Int, height: Int) = {
    var bestNode = Rect(0, 0, 0, 0)
    var bestAreaFit = Int.MaxValue
    var bestShortSideFit = Int.MaxValue

    for (rect <- freeRectangles) {
      val areaFit = rect.width * rect.height - width * height;

      // Try to place the rectangle in upright (non-flipped) orientation.
      if (rect.width >= width && rect.height >= height) {
        val leftoverHoriz = math.abs(rect.width - width);
        val leftoverVert = math.abs(rect.height - height);
        val shortSideFit = math.min(leftoverHoriz, leftoverVert);

        if (areaFit < bestAreaFit || (areaFit == bestAreaFit && shortSideFit < bestShortSideFit)) {
          bestNode = Rect(rect.x, rect.y, width, height)
          bestShortSideFit = shortSideFit;
          bestAreaFit = areaFit;
        }
      }

      if (rect.width >= height && rect.height >= width) {
        val leftoverHoriz = math.abs(rect.width - height);
        val leftoverVert = math.abs(rect.height - width);
        val shortSideFit = math.min(leftoverHoriz, leftoverVert);

        if (areaFit < bestAreaFit || (areaFit == bestAreaFit && shortSideFit < bestShortSideFit)) {
          bestNode = Rect(rect.x, rect.y, width, height)
          bestShortSideFit = shortSideFit;
          bestAreaFit = areaFit;
        }
      }
    }
    (bestNode, bestAreaFit, bestShortSideFit)
  }

  /// Returns 0 if the two intervals i1 and i2 are disjoint, or the length of their overlap otherwise.
  private def CommonIntervalLength(i1start: Int, i1end: Int, i2start: Int, i2end: Int) =
    if (i1end < i2start || i2end < i1start) 0 else math.min(i1end, i2end) - math.max(i1start, i2start)

  private def FindPositionForNewNodeContactPoint(width: Int, height: Int) = {
    var bestNode = Rect(0, 0, 0, 0)
    var bestContactScore = -1

    for (rect <- freeRectangles) {
      // Try to place the rectangle in upright (non-flipped) orientation.
      if (rect.width >= width && rect.height >= height) {
        val score = ContactPointScoreNode(rect.x, rect.y, width, height);
        if (score > bestContactScore) {
          bestNode = Rect(rect.x, rect.y, width, height)
          bestContactScore = score;
        }
      }
      if (rect.width >= height && rect.height >= width) {
        val score = ContactPointScoreNode(rect.x, rect.y, height, width);
        if (score > bestContactScore) {
          bestNode = Rect(rect.x, rect.y, width, height)
          bestContactScore = score;
        }
      }
    }
    (bestNode, bestContactScore)
  }

  /// @return True if the free node was split.
  private def splitFreeNode(freeNode: Rect, usedNode: Rect): Boolean = {
    // Test with SAT if the rectangles even intersect.
    if (usedNode.x >= freeNode.x + freeNode.width || usedNode.x + usedNode.width <= freeNode.x ||
      usedNode.y >= freeNode.y + freeNode.height || usedNode.y + usedNode.height <= freeNode.y)
      return false

    if (usedNode.x < freeNode.x + freeNode.width && usedNode.x + usedNode.width > freeNode.x) {
      // New node at the top side of the used node.
      if (usedNode.y > freeNode.y && usedNode.y < freeNode.y + freeNode.height)
        freeRectangles += Rect(freeNode.x, freeNode.y, freeNode.width, usedNode.y - freeNode.y)

      // New node at the bottom side of the used node.
      if (usedNode.y + usedNode.height < freeNode.y + freeNode.height)
        freeRectangles += Rect(freeNode.x, usedNode.y + usedNode.height, freeNode.width, freeNode.y + freeNode.height - (usedNode.y + usedNode.height))
    }

    if (usedNode.y < freeNode.y + freeNode.height && usedNode.y + usedNode.height > freeNode.y) {
      // New node at the left side of the used node.
      if (usedNode.x > freeNode.x && usedNode.x < freeNode.x + freeNode.width)
        freeRectangles += Rect(freeNode.x, freeNode.y, usedNode.x - freeNode.x, freeNode.height)

      // New node at the right side of the used node.
      if (usedNode.x + usedNode.width < freeNode.x + freeNode.width)
        freeRectangles += Rect(usedNode.x + usedNode.width, freeNode.y, freeNode.x + freeNode.width - (usedNode.x + usedNode.width), freeNode.height)
    }
    true
  }

  /// Goes through the free rectangle list and removes any redundant entries.
  private def pruneFreeList() {
    val newRects = freeRectangles.filterNot( rect =>
      freeRectangles.exists( r => isContainedIn(rect, r) && r != rect )
    )
    freeRectangles.clear()
    freeRectangles ++= newRects
  }

}

object MaxRectsBinPack {

  class OutOfSpace extends Exception
  case class Rect(x: Int, y: Int, width: Int, height: Int)
  private case class RectSize(width: Int, height: Int)

  private class DisjointRectCollection {
    val rects = ListBuffer[Rect]()

    def add(r: Rect): Boolean = {
      // Degenerate rectangles are ignored.
      if (r.width == 0 || r.height == 0)
        true
      else if (!disjoint(r))
        false
      else {
        rects += r
        true
      }
    }

    def clear() = rects.clear()

    def disjoint(r: Rect): Boolean = {
      // Degenerate rectangles are ignored.
      if (r.width == 0 || r.height == 0)
        return true;

      for (rect <- rects)
        if (!MaxRectsBinPack.disjoint(rect, r))
          return false
      true
    }

  }
  
  /** Returns true if a is contained in b. */
  private def isContainedIn(a: Rect, b: Rect): Boolean =
    a.x >= b.x && a.y >= b.y && a.x + a.width <= b.x + b.width && a.y + a.height <= b.y + b.height

  private def disjoint(a: Rect, b: Rect) =
    a.x + a.width <= b.x || b.x + b.width <= a.x || a.y + a.height <= b.y || b.y + b.height <= a.y
}

/** Specifies the different heuristic rules that can be used when deciding where to place a new rectangle. */
sealed abstract class FreeRectChoiceHeuristic
/** -BSSF: Positions the rectangle against the short side of a free rectangle into which it fits the best. */
object RectBestShortSideFit extends FreeRectChoiceHeuristic
/** -BLSF: Positions the rectangle against the long side of a free rectangle into which it fits the best. */
object RectBestLongSideFit extends FreeRectChoiceHeuristic
/** -BAF: Positions the rectangle into the smallest free rect into which it fits. */
object RectBestAreaFit extends FreeRectChoiceHeuristic
/** -BL: Does the Tetris placement. */
object RectBottomLeftRule extends FreeRectChoiceHeuristic
/** -CP: Choosest the placement where the rectangle touches other rects as much as possible. */
object RectContactPointRule extends FreeRectChoiceHeuristic
