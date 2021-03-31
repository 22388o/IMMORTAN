package immortan.sqlite


case class DBInterfaceSQLiteGeneral(connection: java.sql.Connection) extends DBInterface {
  def change(sql: String, params: Object*): Unit = change(makePreparedQuery(sql), params:_*)

  def change(stmt: PreparedQuery, params: Object*): Unit = stmt.bound(params:_*).executeUpdate

  def select(sql: String, params: String*): RichCursor = select(makePreparedQuery(sql), params:_*)

  def select(stmt: PreparedQuery, params: String*): RichCursor = stmt.bound(params:_*).executeQuery

  def makePreparedQuery(sql: String): PreparedQuery = PreparedQuerySQLiteGeneral(connection prepareStatement sql)

  def txWrap[T](run: => T): T = {
    val old = connection.getAutoCommit
    connection.setAutoCommit(false)

    try {
      val runResult = run
      connection.commit
      runResult
    } catch {
      case error: Throwable =>
        connection.rollback
        throw error
    } finally {
      connection.setAutoCommit(old)
    }
  }
}
