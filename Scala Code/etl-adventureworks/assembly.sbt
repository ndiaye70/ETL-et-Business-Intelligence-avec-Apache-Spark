// Ajouter cette configuration pour résoudre les conflits d'assembly
import sbtassembly.AssemblyPlugin.autoImport._

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case PathList("module-info.class") => MergeStrategy.discard
  case PathList("google", "protobuf", _ @_*) => MergeStrategy.first
  case PathList("org", "apache", "logging", _ @_*) => MergeStrategy.first
  case "reference.conf" => MergeStrategy.concat
  case "application.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}
