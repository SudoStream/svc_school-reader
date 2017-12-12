package io.sudostream.schoolreader

trait MiniKubeHelper {
  val mongoKeystorePassword: String = try {
    sys.env("MONGODB_KEYSTORE_PASSWORD")
  } catch {
    case e: Exception => ""
  }
  val isMinikubeRun: Boolean = try {
    if (sys.env("MINIKUBE_RUN") == "true") {
      println("MINIKUBE = yes")
      System.setProperty("javax.net.ssl.keyStore", "/etc/ssl/cacerts")
      System.setProperty("javax.net.ssl.keyStorePassword", mongoKeystorePassword)
      System.setProperty("javax.net.ssl.trustStore", "/etc/ssl/cacerts")
      System.setProperty("javax.net.ssl.trustStorePassword", mongoKeystorePassword)
      true
    } else {
      println("MINIKUBE = no")
      false
    }
  }
  catch {
    case e: Exception => ""
      false
  }

}
