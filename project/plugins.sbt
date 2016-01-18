addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.3")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.0")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.0.3")
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.3")

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.4")