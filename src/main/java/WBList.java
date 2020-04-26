import java.util.ArrayList;

/**
 * A List that can be used as either black or white.
 */
public class WBList<T> {
  private boolean isWhiteList;
  private boolean isBlackList;
  private ArrayList<T> list;

  /**
   * One-arg constructor.
   *
   * @param isWhiteList if this is a whitelist or not (blacklist)
   */
  public WBList(boolean isWhiteList) {
    this.isBlackList = !isWhiteList;
    this.isWhiteList = isWhiteList;
    list = new ArrayList<>();
  }

  /**
   * Add an element to this WBList
   *
   * @param element what to add
   */
  public void add(T element) {
    list.add(element);
  }

  /**
   * Remove and element from this WBList
   *
   * @param element what to remove
   */
  public void remove(T element) {
    list.removeIf(e -> e.equals(element));
  }

  /**
   * Reset the WBList contents.
   */
  public void reset() {
    list = new ArrayList<>();
  }

  /**
   * Reset the WBList contents and set to specified type.
   *
   * @param isWhiteList which type
   */
  public void reset(boolean isWhiteList) {
    list = new ArrayList<>();

    this.isWhiteList = isWhiteList;
    this.isBlackList = !isWhiteList;
  }

}
