let dropdown = document.getElementsByClassName("el-header");

function makeMeActive(thisElem) {
    let dropdownContent = thisElem.nextElementSibling;
    dropdownContent.classList.toggle("activeS");
}

for (let i = 0; i < dropdown.length; i++) {
    dropdown[i].addEventListener("click", function() {
        makeMeActive(this);
    });
}
