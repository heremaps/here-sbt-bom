## Changes


## 1.0.32

Add [CHANGELOG.md](./CHANGELOG.md)

## 1.0.31

**Allow BOM module to be inferred from setting key**

Support use case like:

```
lazy val bomModuleID = settingKey[ModuleID]("bom module ID")
bomModuleID:= ModuleID(organization, name, version)
lazy val bom = Bom.dependencies(bomModuleID)
```

when, for example, `version` may not be hardcoded into the SBT build and is obtained from pom file like in [apache/spark#52760](https://github.com/apache/spark/pull/52760#discussion_r2469376079)

## 1.0.30

SDKJAVA-522: FIX url and local path

Fixes [IvyPomLocator fails when Ivy cache contains remote URL instead of local file path](https://github.com/heremaps/here-sbt-bom/issues/44)


## 1.0.29

Update readme (Scala Version support)