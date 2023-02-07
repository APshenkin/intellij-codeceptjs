# IntelliJ Codeceptjs integration plugin
Integrates <b>Codecept.io</b> under the common Intellij test framework.

## Credits
Big thanks for [Mikhail Bolotov](https://github.com/mbolotov) for reference implementation from [Cypress](https://github.com/mbolotov/intellij-cypress/)

## Compatibility
As the plugin depends on *JavaLanguage* and *NodeJS* plugins, so it requires a commercial version of IDEA (Ultimate, WebStorm etc) 
## Install
Plugin can be installed from the Jetbrains Marketplace. Open '*Settings/Preferences* -> *Plugins*' menu item and type '**Codeceptjs**' in the search bar. See [here](https://www.jetbrains.com/help/idea/managing-plugins.html) for details.
### Test run configurations
Plugin introduces a dedicated Codeceptjs [run configuration](https://www.jetbrains.com/help/idea/run-debug-configuration.html) type
You can create a run config from either file view (directory, spec file) or directly from the code

| file view                      | code view                      |
|--------------------------------|--------------------------------|
| ![](./media/createFromDir.jpg) | ![](./media/createFromSrc.jpg) |

### Running tests
Just start your configuration. You can watch test status live on the corresponding tab:   
![](./media/run.png)

You can navigate from a test entry in the test tab to the source code of this test just by clicking on it.<br>

### File Structure
Your tests now are displayed in IDEA File Structure tab, where you can quick navigate and use fuzzy search
![](./media/fileStructure.jpg)

### Limitations:
1. [Data Driven Tests](https://codecept.io/advanced/#data-driven-tests) run configuration allow you only to run all cases with all data sets. This is because data can be any js object, and it's hard to get access to final string that will be added to test name. If you have ideas, how to implement this, you are welcome to create PR for it 🙃

### Setting up plugin with mocha-multi reporter
This plugin is compatible with mocha-multi reporter that means that you can set both CodeceptJS and this plugin outputs exist at the same time. 
This plugin will automatically detect `mocha-multi` package in dependencies and enable it as reporter if it was installed
To do this you should install both [mocha-multi](https://www.npmjs.com/package/mocha-multi) and [codeceptjs-intellij-reporter](https://www.npmjs.com/package/codeceptjs-intellij-reporter)
as dependency

```bash
npm i mocha-multi
npm i codeceptjs-intellij-reporter
```

Then in your codeceptjs config you should specify reporters configurations. Plugin will set `IJ_CODECEPTJS_MOCHA_MULTI` environment variable,
so you can check that this variable exist to add `codeceptjs-intellij-reporter` here is an example how it can be organized (note that JS config is used):
```js
const reporters = {
  'codeceptjs-cli-reporter': {
    stdout: '-',
    options: {
      verbose: false,
      steps: true,
      noreverse: true,
      debug: false,
    },
  },
};

if (process.env.IJ_CODECEPTJS_MOCHA_MULTI) {
  reporters['codeceptjs-intellij-reporter'] = { stdout: '-' };
}

exports.config = {
  tests: './tests/**/*_test.{js,ts}',

  timeout: 10000,
  output: './tests-output',

  helpers: {
    // some helpers specification
  },
  mocha: {
    reporterOptions: reporters,
  },
};
```


## Build plugin from the sources
```bash
./gradlew buildPlugin
````
## Run
Either start IDE bundled with plugin via gradle:
```bash
./gradlew runIde
```                                             
Or install built plugin manually in the Settings->Plugin section of IDEA

Path to jar : `intellij-codeceptjs/build/libs/intellij-codeceptjs-x.x.jar`
