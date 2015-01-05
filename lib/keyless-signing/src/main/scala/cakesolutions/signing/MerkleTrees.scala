package cakesolutions.signing

import scalaz._

// TODO: implement ideas of "Fractal Merkle Tree Representation and Traversal" by M.Jakobsson, T.Leighton, S.Micali and M.Szydlo
trait MerkleTrees extends TreeFunctions {
  this: HashingFunction =>

  import SignatureProtocol._

  protected case class State(iv: Hash = random, leafHash: Hash = zero, roots: List[MerkleTree] = List.empty[MerkleTree], offset: Int = 0)

  def append[D](data: D, state: State): (Hash, State) = {
    val State(iv, leafHash, roots, _) = state
    val dataHash = hash[D](data)
    val maskingHash = hash[Hash](leafHash, iv)
    val newLeafHash = hash[Hash](maskingHash, dataHash, hash[Int](0))
    val newRoots = balance(roots :+ leaf(TreeNode(newLeafHash, 0)))

    (reduce(newRoots).rootLabel.hash, State(iv, newLeafHash, newRoots))
  }

  protected def reduce(roots: List[MerkleTree]): MerkleTree = {
    require(roots.nonEmpty, "we can not reduce a Merkle tree with no roots")

    roots match {
      case List(root) =>
        root

      case initialRoots :+ root1 :+ root2 =>
        val rootLevel = root1.rootLabel.level.max(root2.rootLabel.level) + 1
        val rootHash = hash(root1.rootLabel.hash, root2.rootLabel.hash, hash(rootLevel))
        reduce(initialRoots :+ node(TreeNode(rootHash, rootLevel), Stream(root1, root2)))
    }
  }

  protected def balance(roots: List[MerkleTree]): List[MerkleTree] = {
    require(roots.nonEmpty, "we can not balance a Merkle tree with no roots")

    roots match {
      case initialRoots :+ root1 :+ root2 if root1.rootLabel.level == root2.rootLabel.level =>
        val rootLevel = root1.rootLabel.level.max(root2.rootLabel.level) + 1
        val rootHash = hash(root1.rootLabel.hash, root2.rootLabel.hash, hash(rootLevel))
        balance(initialRoots :+ node(TreeNode(rootHash, rootLevel), Stream(root1, root2)))

      case _ =>
        roots
    }
  }

  def hashChain(position: Int, roots: List[MerkleTree]): List[Direction] = {
    require(position >= 0, "position must be positive")

    val tree = reduce(balance(roots))

    require(position < math.pow(2, tree.rootLabel.level), "position must be within the Merkle tree")

    hashChain(position, tree.loc)
  }

  // Walk the tree via a binary chop
  def hashChain(position: Int, focus: TreeLoc[TreeNode]): List[Direction] = {
    require(position >= 0, "position must be positive")

    if (position < math.pow(2, focus.tree.rootLabel.level)) {
      if (focus.lefts.isEmpty) {
        List.empty[Direction]
      } else {
        val child = focus.left.get
        Left(child.tree.rootLabel.hash, child.tree.rootLabel.level) :: hashChain(position, child)
      }
    } else {
      if (focus.rights.isEmpty) {
        List.empty[Direction]
      } else {
        val child = focus.right.get
        Right(child.tree.rootLabel.hash, child.tree.rootLabel.level) :: hashChain(position - math.pow(2, focus.tree.rootLabel.level-1).toInt, child)
      }
    }
  }

  def rootHash[R](data: R, path: List[Direction]): Hash = {
    path.zipWithIndex.foldLeft((hash(data), 0)) {
      case ((root, level), (direction, index)) =>
        val l = level + path(index).level + 1
        path(index) match {
          case Left(child, _) =>
            (hash(root, child, l), l)
          case Right(child, _) =>
            (hash(child, root, l), l)
        }
    }._1
  }

}
