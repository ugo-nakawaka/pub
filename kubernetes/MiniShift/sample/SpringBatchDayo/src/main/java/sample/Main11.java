package sample;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.style.ToStringCreator;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import lombok.Data;
import sample.config.DataSourceConfiguration;
import sample.config.EnvironmentConfiguration;
import sample.config.JdbcTemplateConfiguration;
import sample.config.SpringBatchConfig;

public class Main11 {

	@Data
	public static class COLUMNS {

		public String TABLE_SCHEMA;
		public String COLUMN_NAME;
		public String COLUMN_TYPE;
		public String ORDINAL_POSITION;
		public String COLUMN_KEY;

	}

	static String generateSelectSql(String tableName, List<COLUMNS> list) {

		final String joinedColumnNames = list.stream().map(COLUMNS::getCOLUMN_NAME).collect(Collectors.joining(","));

		final String[] orderBy = list.stream().filter(col -> "PRI".equals(col.COLUMN_KEY)).map(COLUMNS::getCOLUMN_NAME)
				.toArray(String[]::new);

		final String sOrderBy = String.join(",", orderBy);

		final StringBuilder builder = new StringBuilder();
		builder.append("SELECT ");
		builder.append(joinedColumnNames).append(" ");
		builder.append("FROM ").append(tableName).append(" ");
		if (orderBy.length != 0) {
			builder.append("ORDER BY ").append(sOrderBy).append(" ");
		}
		builder.append("LIMIT ?, ? ");
		return builder.toString();
	}

	static String generateInsertSql(String tableName, List<COLUMNS> list) {

		final String joinedColumnsName = list.stream().map(cs -> {
			return ":" + cs.getCOLUMN_NAME();
		}).collect(Collectors.joining(","));
		// final String sColumnsName = String.join(",", columnNames);

		final StringBuilder builder = new StringBuilder();
		builder.append("INSERT INTO ").append(tableName).append(" ");
		builder.append("VALUES(");
		builder.append(joinedColumnsName).append(") ");

		return builder.toString();
	}

	static String makeSqlSelectTableColumn() {

		String sql = "SELECT" + //
				" TABLE_SCHEMA" + //
				", TABLE_NAME" + //
				", COLUMN_NAME" + //
				", COLUMN_TYPE" + //
				", ORDINAL_POSITION" + //
				", IS_NULLABLE" + //
				", COLUMN_KEY" + //
				", COLUMN_DEFAULT" + //
				", EXTRA" + //
				", GENERATION_EXPRESSION " + //
				"FROM" + //
				"  INFORMATION_SCHEMA.COLUMNS " + //
				"WHERE" + //
				"  TABLE_SCHEMA = ?" + //
				"  AND TABLE_NAME = ?" + //
				"";

		return sql;

	}

	public static void main(String[] args) throws ClassNotFoundException {

		final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(SpringBatchConfig.class);
		context.register(DataSourceConfiguration.class);
		context.register(EnvironmentConfiguration.class);
		context.register(JdbcTemplateConfiguration.class);
		context.scan("sample.custom");
		context.refresh();

		final JdbcTemplate jdbcTemplate01 = (JdbcTemplate) context.getBean("jdbcTemplate01");
		final JdbcTemplate jdbcTemplate02 = (JdbcTemplate) context.getBean("jdbcTemplate02");

		final Function<String, String> generateInsertSql = (tableName) -> {

			final String schemaName = "employees";
			// final String tableName = "employees";
			;
			final PreparedStatementSetter preparedStatementSetter = new PreparedStatementSetter() {
				public void setValues(PreparedStatement preparedStatement) throws SQLException {
					preparedStatement.setString(1, schemaName);
					preparedStatement.setString(2, tableName);
				}
			};

			final RowMapper<COLUMNS> rowMapper = new DataClassRowMapper<>(COLUMNS.class);
			List<COLUMNS> a = jdbcTemplate01.query(makeSqlSelectTableColumn(), preparedStatementSetter, rowMapper);
			// System.out.println(a);
			String sql = generateInsertSql(tableName, a);
			return sql;
		};

		Function<String, List<Map<String, Object>>> testSql = (sql) -> {
			return jdbcTemplate01.queryForList(sql, 1, 2);
		};

		final List<Map<String, Object>> listTable = jdbcTemplate01.queryForList(
				"select TABLE_SCHEMA ,TABLE_NAME from INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA = 'employees'");

		listTable.forEach(m -> {
			final String schemaName = "employees";
			final String tableName = (String) m.get("TABLE_NAME");
			final String sql = generateInsertSql.apply(tableName);

			System.out.println("==========>" + sql);
			// testSql.apply(sql).forEach(System.out::println);;
		});

	}

}
