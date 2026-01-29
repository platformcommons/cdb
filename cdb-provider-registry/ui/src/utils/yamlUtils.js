export function isValidYaml(yamlString) {
  try {
    jsyaml.load(yamlString);
    return true;
  } catch (e) {
    return false;
  }
}

export function copyYamlToDesigner(yamlContent) {
  if (yamlContent && isValidYaml(yamlContent)) {
    window.getApiDesignerYaml = () => yamlContent;
  }
}