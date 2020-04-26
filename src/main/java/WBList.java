import java.util.ArrayList;

/**
 * A List that can be used as either black or white.
 */
public class WBList<T> {
  private boolean isWhiteList;
  private boolean isBlackList;
  private ArrayList<T> list;

  WBList(boolean isWhiteList) {
    this.isBlackList = !isWhiteList;
    this.isWhiteList = isWhiteList;
    list = new ArrayList<>();
  }

}
