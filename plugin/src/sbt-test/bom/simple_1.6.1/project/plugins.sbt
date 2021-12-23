addSbtPlugin("com.here.platform.artifact" % "sbt-resolver" % "2.0.24")

sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("com.here.platform" % "sbt-bom" % x)
  case _ => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}