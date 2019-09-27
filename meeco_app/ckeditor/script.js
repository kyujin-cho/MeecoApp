let editor;

function MeecoUploadAdapterPlugin(editor) {
  editor.plugins.get("FileRepository").createUploadAdapter = loader => {
    // Configure the URL to the upload script in your back-end here!
    return new MeecoUploadAdapter(loader);
  };
}

window.onSetCategories = (categories) => {
  const select = document.getElementById('categories')
  select.addEventListener('change', (ev) => {
    const selectedIndex = select.selectedIndex
    FTcategoryChanged.postMessage(selectedIndex.toString())
  })
  categories.forEach((item, index) => {
    const option = document.createElement('option')
    option.value = index.toString()
    option.innerHTML = item
    select.appendChild(option)
  })
}

ClassicEditor.create(document.querySelector("#editor"), {
  extraPlugins: [MeecoUploadAdapterPlugin]
})
  .then(newEditor => {
    editor = newEditor;

    editor.model.document.on("change:data", () => {
      console.log("The data has changed!");
      FTarticleChanged.postMessage(editor.getData());
    });
  })
  .catch(error => {
    console.error(error);
  });

document.getElementById("title").addEventListener("change", e => {
  FTtitleChanged.postMessage(e.target.value);
});
