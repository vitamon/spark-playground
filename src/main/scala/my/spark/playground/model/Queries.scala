package my.spark.playground.model

object Queries {

  // 10 countries with highest number of airports (with count) and countries with lowest number of airports
  val countriesToAirports = """
      (SELECT countries.name, COUNT(DISTINCT airports.name) as cnt
      FROM countries, airports
      WHERE
         airports.iso_country = countries.code
      GROUP BY
         countries.name
      ORDER BY cnt DESC
      LIMIT 10)
      UNION
      (SELECT countries.name, COUNT(DISTINCT airports.name) as cnt
      FROM countries, airports
      WHERE
         airports.iso_country = countries.code
      GROUP BY
         countries.name
      ORDER BY cnt ASC
      LIMIT 10)
      ORDER BY cnt DESC
     """

  // Type of runways (as indicated in "surface" column) per country
  val topAirports = """
      SELECT countries.name, runways.surface
      FROM countries, runways, airports
      WHERE
         airports.iso_country = countries.code
         AND airports.id = runways.airport_ref
      GROUP BY
         countries.name,
         runways.surface
      ORDER BY countries.name
     """

  // Top 10 most common runway identifications (indicated in "le_ident" column)
  val topRunways = """
      SELECT le_ident, COUNT(le_ident) as cnt
      FROM runways
      GROUP BY
         le_ident
      ORDER BY cnt DESC
      LIMIT 10
    """

}
