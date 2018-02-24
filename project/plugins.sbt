resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += Resolver.url("bintray-sbt-plugin-releases", url("https://dl.bintray.com/content/sbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns)

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.2.0")
