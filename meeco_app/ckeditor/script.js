let editor;

function MeecoUploadAdapterPlugin(editor) {
  editor.plugins.get("FileRepository").createUploadAdapter = loader => {
    // Configure the URL to the upload script in your back-end here!
    return new MeecoUploadAdapter(loader);
  };
}

ClassicEditor.create(document.querySelector("#editor"), {
  extraPlugins: [MeecoUploadAdapterPlugin]
})
  .then(newEditor => {
    editor = newEditor;

    editor.model.document.on("change:data", () => {
      console.log("The data has changed!");
      window.webkit.messageHandlers.article.postMessage({
        event: "ContentChanged",
        value: editor.getData()
      });
    });
  })
  .catch(error => {
    console.error(error);
  });

document.getElementById("title").addEventListener("change", e => {
  window.webkit.messageHandlers.article.postMessage({
    event: "TitleChanged",
    value: e.target.value
  });
});
